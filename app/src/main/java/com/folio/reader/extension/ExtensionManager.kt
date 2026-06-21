package com.folio.reader.extension

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.folio.reader.source.MediaSource
import dalvik.system.DexClassLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/** Persists which extensions the user has enabled, keyed by package name. */
private val Context.extensionPrefs by preferencesDataStore(name = "extension_prefs")
private fun enabledKey(pkgName: String) = booleanPreferencesKey("enabled_$pkgName")

/**
 * Descriptor dropped next to an extension's code file in the extensions directory,
 * e.g. `com.example.foo.json` describing `com.example.foo.dex`.
 */
@Serializable
private data class ExtensionManifest(
    val pkgName: String,
    val name: String,
    val version: String,
    val factoryClass: String,
    val lang: String = "en",
    val nsfw: Boolean = false,
)

/** A discovered extension, as surfaced to the UI. */
data class Extension(
    val pkgName: String,
    val name: String,
    val version: String,
    val lang: String,
    val isNsfw: Boolean,
    val isEnabled: Boolean,
)

private data class DiscoveredExtension(val manifest: ExtensionManifest, val codeFile: File)

/**
 * Scans a local directory for extensions and dynamically loads the [MediaSource]s
 * each one contributes. An extension is a `<pkgName>.json` manifest paired with a
 * `<pkgName>.dex` (or `.jar`) code file containing a class that implements
 * [SourceFactory]. Enabled/disabled state is tracked in DataStore so it survives
 * rescans and app restarts, and the discovered extension list is exposed as a
 * reactive [StateFlow] for the UI to observe — the same shape Mihon uses for its
 * `installedExtensionsFlow`, minus the package-manager/APK-signing machinery that
 * doesn't apply here.
 */
class ExtensionManager(
    private val context: Context,
    private val scope: CoroutineScope,
) {
    private val extensionsDir = File(context.filesDir, "extensions").apply { mkdirs() }
    private val codeCacheDir = File(context.codeCacheDir, "extension_opt").apply { mkdirs() }
    private val json = Json { ignoreUnknownKeys = true }

    private val discoveredFlow = MutableStateFlow<Map<String, DiscoveredExtension>>(emptyMap())

    /** Cached, already dynamically-loaded sources, by package name, so toggling an
     *  extension on and off doesn't reload its code every time. */
    private val sourcesCache = mutableMapOf<String, List<MediaSource>>()
    private val classLoaderCache = mutableMapOf<String, DexClassLoader>()

    /** Reactive view of every discovered extension and its enabled flag, rebuilt
     *  whenever the directory is rescanned or the user toggles an extension. */
    val extensions: StateFlow<List<Extension>> = combine(
        discoveredFlow,
        context.extensionPrefs.data,
    ) { discovered, prefs ->
        discovered.values.map { (manifest, _) ->
            Extension(
                pkgName = manifest.pkgName,
                name = manifest.name,
                version = manifest.version,
                lang = manifest.lang,
                isNsfw = manifest.nsfw,
                isEnabled = prefs[enabledKey(manifest.pkgName)] ?: false,
            )
        }.sortedBy { it.name }
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    init {
        scanExtensions()
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

    suspend fun setEnabled(pkgName: String, enabled: Boolean) {
        context.extensionPrefs.edit { it[enabledKey(pkgName)] = enabled }
    }

    /**
     * Dynamically loads (and caches) the sources contributed by [pkgName] via its
     * declared [SourceFactory], or returns an empty list if it's unknown or fails
     * to load. Safe to call repeatedly — only the first call per package pays for
     * the classloading.
     */
    suspend fun loadSources(pkgName: String): List<MediaSource> = withContext(Dispatchers.IO) {
        sourcesCache[pkgName]?.let { return@withContext it }
        val discovered = discoveredFlow.value[pkgName] ?: return@withContext emptyList()

        runCatching {
            val classLoader = classLoaderCache.getOrPut(pkgName) {
                DexClassLoader(
                    discovered.codeFile.absolutePath,
                    codeCacheDir.absolutePath,
                    null,
                    context.classLoader,
                )
            }
            val factory = classLoader
                .loadClass(discovered.manifest.factoryClass)
                .getDeclaredConstructor()
                .newInstance() as SourceFactory
            factory.createSources()
        }.onSuccess { sourcesCache[pkgName] = it }.getOrDefault(emptyList())
    }

    /** Removes an extension's manifest and code from disk and forgets any cached sources. */
    fun deleteExtension(pkgName: String) {
        File(extensionsDir, "$pkgName.json").delete()
        File(extensionsDir, "$pkgName.dex").delete()
        File(extensionsDir, "$pkgName.jar").delete()
        sourcesCache.remove(pkgName)
        classLoaderCache.remove(pkgName)
        scanExtensions()
    }
}
