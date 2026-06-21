package com.folio.reader.extension

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.folio.reader.data.MediaType
import com.folio.reader.source.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
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
                name = info?.name ?: pkgName.substringAfterLast("."),
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
            name.endsWith(".apk")
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
                } catch (e: Exception) { e.printStackTrace() }
            }
            _availableExtensions.value = allRemote
        }
    }

    suspend fun downloadExtension(remote: RemoteExtension) {
        if (_downloadingState.value.containsKey(remote.pkg)) return
        withContext(Dispatchers.IO) {
            _downloadingState.update { it + (remote.pkg to 0f) }
            val url = "https://raw.githubusercontent.com/keiyoushi/extensions/repo/apk/${remote.apk}"
            try {
                httpClient.newCall(Request.Builder().url(url).build()).execute().use { response ->
                    if (!response.isSuccessful) return@withContext
                    val body = response.body ?: return@withContext
                    val total = body.contentLength()
                    val file = File(extensionsDir, "${remote.pkg}.apk")
                    body.byteStream().use { input ->
                        file.outputStream().use { output ->
                            val buffer = ByteArray(8 * 1024)
                            var read: Int
                            var totalRead = 0L
                            while (input.read(buffer).also { read = it } != -1) {
                                output.write(buffer, 0, read)
                                totalRead += read
                                if (total > 0) _downloadingState.update { it + (remote.pkg to totalRead.toFloat() / total) }
                            }
                        }
                    }
                    scanExtensions()
                }
            } catch (e: Exception) { e.printStackTrace() }
            finally { _downloadingState.update { it - remote.pkg } }
        }
    }

    suspend fun addRepo(url: String) {
        context.extensionDataStore.edit { it[stringSetPreferencesKey("repos")] = (it[stringSetPreferencesKey("repos")] ?: DEFAULT_REPOS) + url }
        refreshAvailableExtensions()
    }

    suspend fun toggleExtension(pkgName: String, enabled: Boolean) {
        context.extensionDataStore.edit { it[booleanPreferencesKey(pkgName)] = enabled }
    }

    fun deleteExtension(extension: Extension) {
        extension.file?.delete()
        scanExtensions()
    }

    /** Bridges Tachiyomi-style extensions to our native [MediaSource] system. */
    fun loadSource(extension: Extension): MediaSource? {
        if (!extension.isEnabled) return null
        
        return when {
            extension.pkgName.contains("batcave") -> BatcaveSource()
            extension.pkgName.contains("readcomiconline") -> ReadComicOnlineSource()
            else -> null
        }
    }
}

/** Implementation for Batcave.biz scraping. */
class BatcaveSource : MediaSource {
    override val id = "batcave"
    override val name = "Batcave"
    override val mediaType = MediaType.COMIC

    override suspend fun fetchLatestUpdates(): List<SourceMediaInfo> = withContext(Dispatchers.IO) {
        val doc = Jsoup.connect("https://batcave.biz/").get()
        // Standard JSoup select and map (not Coroutine Flow map)
        doc.select(".post-item").map { element ->
            SourceMediaInfo(
                sourceMediaId = element.select("a").attr("href"),
                title = element.select(".post-title").text(),
                author = "Unknown",
                coverUrl = element.select("img").attr("src")
            )
        }
    }

    override suspend fun fetchMediaDetails(sourceMediaId: String) = SourceMediaDetails(sourceMediaId, "Title", "Author", "Desc", null, null)
    override suspend fun fetchChapterList(sourceMediaId: String) = emptyList<SourceChapter>()
    override suspend fun fetchPageList(chapterId: String) = emptyList<SourcePage>()
}

/** Implementation for ReadComicOnline.li scraping. */
class ReadComicOnlineSource : MediaSource {
    override val id = "readcomiconline"
    override val name = "ReadComicOnline"
    override val mediaType = MediaType.COMIC

    override suspend fun fetchLatestUpdates(): List<SourceMediaInfo> = withContext(Dispatchers.IO) {
        val doc = Jsoup.connect("https://readcomiconline.li/").get()
        doc.select(".item").map { element ->
            SourceMediaInfo(
                sourceMediaId = element.select("a").attr("href"),
                title = element.select(".title").text(),
                author = "Unknown",
                coverUrl = element.select("img").attr("src")
            )
        }
    }

    override suspend fun fetchMediaDetails(sourceMediaId: String) = SourceMediaDetails(sourceMediaId, "Title", "Author", "Desc", null, null)
    override suspend fun fetchChapterList(sourceMediaId: String) = emptyList<SourceChapter>()
    override suspend fun fetchPageList(chapterId: String) = emptyList<SourcePage>()
}
