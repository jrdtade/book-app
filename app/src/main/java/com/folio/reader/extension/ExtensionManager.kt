package com.folio.reader.extension

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.folio.reader.source.MediaSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

/** Persistently track enabled/disabled state and repos. */
private val Context.extensionDataStore by preferencesDataStore(name = "extension_prefs")

private val DEFAULT_REPOS = setOf("https://raw.githubusercontent.com/keiyoushi/extensions/repo/index.min.json")

@Serializable
data class RemoteExtension(
    val name: String,
    val pkg: String,
    val apk: String,
    val version: String,
    val lang: String? = null,
    val nsfw: Int = 0
)

data class Extension(
    val pkgName: String,
    val name: String,
    val version: String,
    val isEnabled: Boolean,
    val isInstalled: Boolean,
    val file: File? = null,
    val remoteInfo: RemoteExtension? = null,
    val isDownloading: Boolean = false,
    val progress: Float = 0f
)

/**
 * Scans for and loads external media sources.
 */
class ExtensionManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val httpClient: OkHttpClient
) {
    private val extensionsDir = File(context.filesDir, "extensions").apply { mkdirs() }
    private val json = Json { ignoreUnknownKeys = true }

    private val _installedFiles = MutableStateFlow<List<File>>(emptyList())
    private val _availableExtensions = MutableStateFlow<List<RemoteExtension>>(emptyList())
    private val _downloadingState = MutableStateFlow<Map<String, Float>>(emptyMap())

    /** All discovered extensions, reactively updated when files or settings change. */
    val extensions: StateFlow<List<Extension>> = combine(
        _installedFiles,
        _availableExtensions,
        _downloadingState,
        context.extensionDataStore.data
    ) { files, remote, downloading, prefs ->
        val remoteMap = remote.associateBy { it.pkg }
        
        val installed = files.map { file ->
            val pkgName = file.nameWithoutExtension
            val info = remoteMap[pkgName]
            Extension(
                pkgName = pkgName,
                name = info?.name ?: pkgName.replaceFirstChar { it.uppercase() },
                version = info?.version ?: "1.0.0",
                isEnabled = prefs[booleanPreferencesKey(pkgName)] ?: true,
                isInstalled = true,
                file = file,
                remoteInfo = info
            )
        }

        val installedPkgs = installed.map { it.pkgName }.toSet()
        val available = remote.filter { it.pkg !in installedPkgs }.map {
            Extension(
                pkgName = it.pkg,
                name = it.name,
                version = it.version,
                isEnabled = false,
                isInstalled = false,
                remoteInfo = it,
                isDownloading = downloading.containsKey(it.pkg),
                progress = downloading[it.pkg] ?: 0f
            )
        }

        (installed + available).sortedBy { it.name }
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    val repos: StateFlow<Set<String>> = context.extensionDataStore.data
        .map { it[stringSetPreferencesKey("repos")] ?: DEFAULT_REPOS }
        .stateIn(scope, SharingStarted.Eagerly, DEFAULT_REPOS)

    init {
        scanExtensions()
        refreshAvailableExtensions()
    }

    fun scanExtensions() {
        _installedFiles.value = extensionsDir.listFiles { _, name ->
            name.endsWith(".apk") || name.endsWith(".json")
        }?.toList() ?: emptyList()
    }

    fun refreshAvailableExtensions() {
        scope.launch(Dispatchers.IO) {
            val allRemote = mutableListOf<RemoteExtension>()
            repos.value.forEach { url ->
                try {
                    val request = Request.Builder().url(url).build()
                    httpClient.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val body = response.body?.string() ?: ""
                            allRemote.addAll(json.decodeFromString<List<RemoteExtension>>(body))
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            _availableExtensions.value = allRemote
        }
    }

    suspend fun addRepo(url: String) {
        context.extensionDataStore.edit { prefs ->
            val current = prefs[stringSetPreferencesKey("repos")] ?: DEFAULT_REPOS
            prefs[stringSetPreferencesKey("repos")] = current + url
        }
        refreshAvailableExtensions()
    }

    suspend fun removeRepo(url: String) {
        context.extensionDataStore.edit { prefs ->
            val current = prefs[stringSetPreferencesKey("repos")] ?: DEFAULT_REPOS
            prefs[stringSetPreferencesKey("repos")] = current - url
        }
        refreshAvailableExtensions()
    }

    suspend fun toggleExtension(pkgName: String, enabled: Boolean) {
        context.extensionDataStore.edit { prefs ->
            prefs[booleanPreferencesKey(pkgName)] = enabled
        }
    }

    suspend fun downloadExtension(remote: RemoteExtension) {
        if (_downloadingState.value.containsKey(remote.pkg)) return
        
        withContext(Dispatchers.IO) {
            _downloadingState.update { it + (remote.pkg to 0f) }
            val url = "https://raw.githubusercontent.com/keiyoushi/extensions/repo/apk/${remote.apk}"
            val request = Request.Builder().url(url).build()
            try {
                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw Exception("Failed to download: ${response.code}")
                    
                    val body = response.body ?: throw Exception("Empty body")
                    val totalBytes = body.contentLength()
                    val file = File(extensionsDir, "${remote.pkg}.apk")
                    
                    body.byteStream().use { input ->
                        file.outputStream().use { output ->
                            val buffer = ByteArray(8 * 1024)
                            var bytesRead: Int
                            var totalRead = 0L
                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                output.write(buffer, 0, bytesRead)
                                totalRead += bytesRead
                                if (totalBytes > 0) {
                                    val progress = totalRead.toFloat() / totalBytes
                                    _downloadingState.update { it + (remote.pkg to progress) }
                                }
                            }
                        }
                    }
                    scanExtensions()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _downloadingState.update { it - remote.pkg }
            }
        }
    }

    /**
     * Dynamically instantiates a [MediaSource] from an extension file.
     */
    fun loadSource(extension: Extension): MediaSource? {
        if (!extension.isEnabled) return null
        
        // If it's a built-in "dummy" for testing or if we don't have the file yet
        if (extension.file == null && extension.remoteInfo == null) return null

        // In a real Tachiyomi implementation, we'd use DexClassLoader here to load
        // the actual scraper logic from the APK. 
        // For this architecture demo, we'll return a dynamic source that simulates
        // the catalog content for the given extension.
        return object : MediaSource {
            override val id: String = extension.pkgName
            override val name: String = extension.name
            override val mediaType: com.folio.reader.data.MediaType = com.folio.reader.data.MediaType.MANGA

            override suspend fun fetchLatestUpdates(): List<com.folio.reader.source.SourceMediaInfo> {
                // Simulate a full catalog of 20 items
                return (1..20).map { i ->
                    com.folio.reader.source.SourceMediaInfo(
                        sourceMediaId = "${extension.pkgName}_item_$i",
                        title = "${extension.name} Content #$i",
                        author = "Author $i",
                        coverUrl = "https://picsum.photos/seed/${extension.pkgName}_$i/300/450"
                    )
                }
            }

            override suspend fun fetchMediaDetails(sourceMediaId: String) = com.folio.reader.source.SourceMediaDetails(
                sourceMediaId = sourceMediaId,
                title = "Detailed Title for $sourceMediaId",
                author = "Author Name",
                description = "This is a rich description fetched from the extension ${extension.name}. " +
                        "In a production environment, this would be scraped from the source website.",
                coverUrl = null,
                genre = "Action, Adventure, Fantasy"
            )

            override suspend fun fetchChapterList(sourceMediaId: String) = (1..50).map { i ->
                com.folio.reader.source.SourceChapter(
                    chapterId = "${sourceMediaId}_ch_$i",
                    title = "Chapter $i",
                    number = i.toFloat()
                )
            }

            override suspend fun fetchPageList(chapterId: String) = (0..10).map { i ->
                com.folio.reader.source.SourcePage(
                    index = i,
                    contentPath = "https://picsum.photos/seed/${chapterId}_$i/800/1200"
                )
            }
        }
    }

    fun deleteExtension(extension: Extension) {
        extension.file?.delete()
        scanExtensions()
    }
}
