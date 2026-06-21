package com.folio.reader.extension

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.folio.reader.source.MediaSource
import dalvik.system.DexClassLoader
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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.net.URI
import eu.kanade.tachiyomi.source.Source as TachiyomiSource
import eu.kanade.tachiyomi.source.SourceFactory as TachiyomiSourceFactory

/** AndroidManifest `<application>` meta-data key real Tachiyomi/Mihon extension
 *  apks use to declare their entry point class(es). */
private const val TACHIYOMI_EXTENSION_CLASS_META_KEY = "tachiyomi.extension.class"

private const val TAG = "ExtensionManager"

/** Persists which extensions the user has enabled and which repos they've added. */
private val Context.extensionPrefs by preferencesDataStore(name = "extension_prefs")
private fun enabledKey(pkgName: String) = booleanPreferencesKey("enabled_$pkgName")
private val CUSTOM_REPOS_KEY = stringSetPreferencesKey("custom_repo_urls")

/** Baked-in fallback so the app always has somewhere to fetch extensions from. */
const val DEFAULT_REPO_URL = "https://raw.githubusercontent.com/keiyoushi/extensions/repo/index.min.json"

/** A source of extension index JSON. [isDefault] repos can't be deleted by the user. */
data class ExtensionRepo(
    val url: String,
    val isDefault: Boolean = false,
)

/**
 * Descriptor dropped next to an extension's code file in the extensions directory,
 * e.g. `com.example.foo.json` describing `com.example.foo.dex`. [factoryClass] is
 * set for extensions built against our own [SourceFactory] contract; it's left
 * null for installed Tachiyomi/Keiyoushi apks, whose real entry point(s) are
 * discovered from the apk's own AndroidManifest meta-data at load time instead.
 */
@Serializable
private data class ExtensionManifest(
    val pkgName: String,
    val name: String,
    val version: String,
    val factoryClass: String? = null,
    val lang: String = "en",
    val nsfw: Boolean = false,
)

/** One row of a repo's `index.min.json` — metadata for an extension available to install. */
@Serializable
private data class RemoteExtensionDto(
    val name: String,
    val pkg: String,
    val apk: String,
    val version: String,
    val lang: String? = null,
    val nsfw: Int = 0,
)

/** A [RemoteExtensionDto] paired with the repo base URL it came from, needed to build its apk link. */
private data class RemoteExtensionEntry(val repoBaseUrl: String, val dto: RemoteExtensionDto)

/** A discovered or remotely-available extension, as surfaced to the UI. */
data class Extension(
    val pkgName: String,
    val name: String,
    val version: String,
    val lang: String,
    val isNsfw: Boolean,
    val isEnabled: Boolean,
    val isInstalled: Boolean,
    val apkUrl: String? = null,
    val isInstalling: Boolean = false,
    /** Set when this extension is enabled but its sources failed to load (bad apk,
     *  a class our Tachiyomi shim doesn't fully support, etc.) — surfaced in the
     *  UI so a failure isn't just "nothing shows up under Sources" with no clue why. */
    val loadError: String? = null,
)

private data class DiscoveredExtension(val manifest: ExtensionManifest, val codeFile: File)

/**
 * Scans a local directory for installed extensions and dynamically loads the
 * [MediaSource]s each one contributes, while also fetching the index of
 * extensions available to install from the user's saved [ExtensionRepo]s
 * (Keiyoushi's repo is baked in as a default). An installed extension is a
 * `<pkgName>.json` manifest paired with a `<pkgName>.dex` (or `.jar`) code file
 * containing a class that implements [SourceFactory]. Enabled state and custom
 * repos are tracked in DataStore so they survive rescans and app restarts, and
 * the merged extension list is exposed as a reactive [StateFlow] for the UI to
 * observe — the same shape Mihon uses for its `installedExtensionsFlow` /
 * `availableExtensionsFlow`, minus the package-manager/APK-signing machinery
 * that doesn't apply here.
 */
class ExtensionManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val httpClient: OkHttpClient,
) {
    private val extensionsDir = File(context.filesDir, "extensions").apply { mkdirs() }
    private val codeCacheDir = File(context.codeCacheDir, "extension_opt").apply { mkdirs() }
    private val json = Json { ignoreUnknownKeys = true }

    private val discoveredFlow = MutableStateFlow<Map<String, DiscoveredExtension>>(emptyMap())
    private val remoteEntriesFlow = MutableStateFlow<List<RemoteExtensionEntry>>(emptyList())
    private val installingFlow = MutableStateFlow<Set<String>>(emptySet())
    private val loadErrorFlow = MutableStateFlow<Map<String, String>>(emptyMap())

    /** Cached, already dynamically-loaded sources, by package name, so toggling an
     *  extension on and off doesn't reload its code every time. */
    private val sourcesCache = mutableMapOf<String, List<MediaSource>>()
    private val classLoaderCache = mutableMapOf<String, DexClassLoader>()

    /** Reactive view of the user's repos: the baked-in default plus any they've added. */
    val repos: StateFlow<List<ExtensionRepo>> = context.extensionPrefs.data
        .map { prefs ->
            val custom = (prefs[CUSTOM_REPOS_KEY] ?: emptySet())
                .sorted()
                .map { ExtensionRepo(it, isDefault = false) }
            listOf(ExtensionRepo(DEFAULT_REPO_URL, isDefault = true)) + custom
        }
        .stateIn(scope, SharingStarted.Eagerly, listOf(ExtensionRepo(DEFAULT_REPO_URL, isDefault = true)))

    /** Reactive view of every extension — installed (local) and available (from
     *  repos) — merged into one list, with installed entries taking priority
     *  when a package appears in both. */
    val extensions: StateFlow<List<Extension>> = combine(
        discoveredFlow,
        context.extensionPrefs.data,
        remoteEntriesFlow,
        installingFlow,
        loadErrorFlow,
    ) { discovered, prefs, remoteEntries, installing, loadErrors ->
        val installed = discovered.values.map { (manifest, _) ->
            Extension(
                pkgName = manifest.pkgName,
                name = manifest.name,
                version = manifest.version,
                lang = manifest.lang,
                isNsfw = manifest.nsfw,
                isEnabled = prefs[enabledKey(manifest.pkgName)] ?: false,
                isInstalled = true,
                isInstalling = manifest.pkgName in installing,
                loadError = loadErrors[manifest.pkgName],
            )
        }

        val installedPkgs = installed.map { it.pkgName }.toSet()
        val available = remoteEntries
            .filter { it.dto.pkg !in installedPkgs }
            .distinctBy { it.dto.pkg }
            .map { entry ->
                Extension(
                    pkgName = entry.dto.pkg,
                    name = entry.dto.name,
                    version = entry.dto.version,
                    lang = entry.dto.lang ?: "en",
                    isNsfw = entry.dto.nsfw != 0,
                    isEnabled = false,
                    isInstalled = false,
                    apkUrl = entry.repoBaseUrl + "apk/" + entry.dto.apk,
                    isInstalling = entry.dto.pkg in installing,
                )
            }

        (installed + available).sortedBy { it.name }
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    init {
        scanExtensions()
        // Re-fetch every saved repo's index whenever the repo list changes (including on startup).
        scope.launch {
            repos.collect { refreshAvailableExtensions() }
        }
    }

    /** Re-scans [extensionsDir] for manifest/code pairs. Call after copying a new
     *  extension into place (e.g. from a file picker or a share-target unzip step). */
    fun scanExtensions() {
        val manifestFiles = extensionsDir.listFiles { f -> f.extension == "json" } ?: emptyArray()

        val found = manifestFiles.mapNotNull { manifestFile ->
            runCatching {
                val manifest = json.decodeFromString<ExtensionManifest>(manifestFile.readText())
                val codeFile = File(extensionsDir, "${manifest.pkgName}.dex")
                    .takeIf { it.exists() }
                    ?: File(extensionsDir, "${manifest.pkgName}.jar").takeIf { it.exists() }
                    ?: File(extensionsDir, "${manifest.pkgName}.apk").takeIf { it.exists() }
                    ?: return@mapNotNull null
                manifest.pkgName to DiscoveredExtension(manifest, codeFile)
            }.getOrNull()
        }.toMap()

        // Drop cached classloaders/sources for extensions that disappeared since the last scan.
        val removed = discoveredFlow.value.keys - found.keys
        removed.forEach {
            sourcesCache.remove(it)
            classLoaderCache.remove(it)
        }
        discoveredFlow.value = found
    }

    /** Re-fetches the index.min.json of every saved repo and merges the results. */
    fun refreshExtensions() {
        scope.launch { refreshAvailableExtensions() }
    }

    private suspend fun refreshAvailableExtensions() = withContext(Dispatchers.IO) {
        val entries = repos.value.flatMap { repo ->
            runCatching {
                val request = Request.Builder().url(repo.url).build()
                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use emptyList()
                    val body = response.body?.string().orEmpty()
                    val dtos = json.decodeFromString<List<RemoteExtensionDto>>(body)
                    val baseUrl = repo.url.substringBeforeLast('/') + "/"
                    dtos.map { RemoteExtensionEntry(baseUrl, it) }
                }
            }.getOrDefault(emptyList())
        }
        remoteEntriesFlow.value = entries
    }

    /** Adds a custom repo if [url] is a well-formed http(s) URL. Returns false (and
     *  doesn't save anything) if validation fails. */
    suspend fun addRepo(url: String): Boolean {
        val trimmed = url.trim()
        if (!isValidRepoUrl(trimmed)) return false
        context.extensionPrefs.edit { prefs ->
            val current = prefs[CUSTOM_REPOS_KEY] ?: emptySet()
            prefs[CUSTOM_REPOS_KEY] = current + trimmed
        }
        return true
    }

    /** No-ops for the default repo — only user-added repos can be removed. */
    suspend fun deleteRepo(repo: ExtensionRepo) {
        if (repo.isDefault) return
        context.extensionPrefs.edit { prefs ->
            val current = prefs[CUSTOM_REPOS_KEY] ?: emptySet()
            prefs[CUSTOM_REPOS_KEY] = current - repo.url
        }
    }

    private fun isValidRepoUrl(url: String): Boolean {
        val uri = runCatching { URI(url) }.getOrNull() ?: return false
        return (uri.scheme == "http" || uri.scheme == "https") && !uri.host.isNullOrBlank()
    }

    suspend fun setEnabled(pkgName: String, enabled: Boolean) {
        context.extensionPrefs.edit { it[enabledKey(pkgName)] = enabled }
    }

    /**
     * Dynamically loads (and caches) the sources contributed by [pkgName], or
     * returns an empty list if it's unknown or fails to load. Safe to call
     * repeatedly — only the first call per package pays for the classloading.
     * Dispatches to one of two loading strategies depending on the extension:
     * - Our own format ([ExtensionManifest.factoryClass] set): instantiate that
     *   class and call its [SourceFactory.createSources] directly.
     * - A real Tachiyomi/Keiyoushi apk (`factoryClass` null): read the apk's own
     *   AndroidManifest meta-data to find its real entry point class(es), load
     *   each one, and wrap the resulting [TachiyomiSource]s in a [TachiyomiSourceAdapter].
     */
    suspend fun loadSources(pkgName: String): List<MediaSource> = withContext(Dispatchers.IO) {
        sourcesCache[pkgName]?.let { return@withContext it }
        val discovered = discoveredFlow.value[pkgName] ?: return@withContext emptyList()

        runCatching {
            if (discovered.manifest.factoryClass != null) {
                loadLocalSources(discovered)
            } else {
                loadTachiyomiSources(discovered)
            }
        }.onSuccess { sources ->
            sourcesCache[pkgName] = sources
            loadErrorFlow.update { it - pkgName }
        }.onFailure { e ->
            Log.e(TAG, "Failed to load sources for $pkgName", e)
            loadErrorFlow.update { it + (pkgName to (e.message ?: e.javaClass.simpleName)) }
        }.getOrDefault(emptyList())
    }

    private fun obtainClassLoader(discovered: DiscoveredExtension): DexClassLoader =
        classLoaderCache.getOrPut(discovered.manifest.pkgName) {
            DexClassLoader(
                discovered.codeFile.absolutePath,
                codeCacheDir.absolutePath,
                null,
                context.classLoader,
            )
        }

    private fun loadLocalSources(discovered: DiscoveredExtension): List<MediaSource> {
        val classLoader = obtainClassLoader(discovered)
        val factory = classLoader
            .loadClass(discovered.manifest.factoryClass)
            .getDeclaredConstructor()
            .newInstance() as SourceFactory
        return factory.createSources()
    }

    /** Mirrors Mihon's `ExtensionLoader`: read the apk's own `tachiyomi.extension.class`
     *  meta-data (no real `pm install` needed — `getPackageArchiveInfo` parses any
     *  apk file on disk) to find the class(es) to instantiate. */
    private fun loadTachiyomiSources(discovered: DiscoveredExtension): List<MediaSource> {
        val pkgInfo = context.packageManager.getPackageArchiveInfo(
            discovered.codeFile.absolutePath,
            PackageManager.GET_META_DATA,
        ) ?: return emptyList()

        val realPkgName = pkgInfo.packageName
        val classNames = pkgInfo.applicationInfo
            ?.metaData
            ?.getString(TACHIYOMI_EXTENSION_CLASS_META_KEY)
            ?.split(";", ",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?: return emptyList()

        val classLoader = obtainClassLoader(discovered)
        return classNames.flatMap { rawName ->
            val className = if (rawName.startsWith(".")) realPkgName + rawName else rawName
            // One multi-source extension declares several classes; a single one
            // failing (e.g. a base class our shim doesn't fully replicate)
            // shouldn't sink every other source the same apk also provides.
            runCatching {
                val instance = classLoader.loadClass(className).getDeclaredConstructor().newInstance()
                when (instance) {
                    is TachiyomiSourceFactory -> instance.createSources().map { TachiyomiSourceAdapter(it) }
                    is TachiyomiSource -> listOf(TachiyomiSourceAdapter(instance))
                    else -> emptyList()
                }
            }.onFailure { e -> Log.e(TAG, "Failed to instantiate $className", e) }
                .getOrDefault(emptyList())
        }
    }

    /** Removes an extension's manifest and code from disk and forgets any cached sources. */
    fun deleteExtension(pkgName: String) {
        File(extensionsDir, "$pkgName.json").delete()
        File(extensionsDir, "$pkgName.dex").delete()
        File(extensionsDir, "$pkgName.jar").delete()
        File(extensionsDir, "$pkgName.apk").delete()
        sourcesCache.remove(pkgName)
        classLoaderCache.remove(pkgName)
        scanExtensions()
    }

    /**
     * Downloads [extension]'s apk from the repo it came from and installs it —
     * i.e. saves the apk into the extensions directory and writes a manifest with
     * no [ExtensionManifest.factoryClass], marking it as a real Tachiyomi/Keiyoushi
     * extension whose entry point(s) get discovered from its own AndroidManifest
     * the first time [loadSources] is called for it. Returns false on any failure
     * (bad URL, network error, etc.) without leaving partial files behind.
     */
    suspend fun installExtension(extension: Extension): Boolean {
        val apkUrl = extension.apkUrl ?: return false
        installingFlow.update { it + extension.pkgName }
        return try {
            withContext(Dispatchers.IO) {
                val request = Request.Builder().url(apkUrl).build()
                httpClient.newCall(request).execute().use { response ->
                    check(response.isSuccessful) { "HTTP ${response.code}" }
                    val body = checkNotNull(response.body)
                    val apkFile = File(extensionsDir, "${extension.pkgName}.apk")
                    body.byteStream().use { input -> apkFile.outputStream().use { input.copyTo(it) } }
                }

                val manifest = ExtensionManifest(
                    pkgName = extension.pkgName,
                    name = extension.name,
                    version = extension.version,
                    factoryClass = null,
                    lang = extension.lang,
                    nsfw = extension.isNsfw,
                )
                File(extensionsDir, "${extension.pkgName}.json").writeText(json.encodeToString(manifest))
            }
            scanExtensions()
            // Enable immediately so the source shows up under "Sources" without an extra tap.
            setEnabled(extension.pkgName, true)
            true
        } catch (e: Exception) {
            File(extensionsDir, "${extension.pkgName}.apk").delete()
            false
        } finally {
            installingFlow.update { it - extension.pkgName }
        }
    }
}
