package com.folio.reader.source

import com.folio.reader.data.BookDao
import com.folio.reader.data.MediaType
import com.folio.reader.extension.ExtensionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Holds every known [MediaSource], local or remote, keyed for lookup by the
 *  library/reader UI without those layers needing to know concrete source types. */
class SourceRegistry(
    private val bookDao: BookDao,
    private val extensionManager: ExtensionManager,
    scope: CoroutineScope
) {
    private val builtInSources = listOf(
        LocalEpubSource(bookDao),
        CbzCbrSource(bookDao, MediaType.COMIC),
        CbzCbrSource(bookDao, MediaType.MANGA),
        StubNetworkSource(id = "stub_manga_network", name = "Network (stub)", mediaType = MediaType.MANGA),
    )

    /** Reactively rebuilds whenever an extension is discovered, removed, or toggled.
     *  Only enabled extensions get their [com.folio.reader.extension.SourceFactory]
     *  dynamically loaded; disabled ones are skipped entirely. */
    val sources: StateFlow<List<MediaSource>> = extensionManager.extensions
        .map { extensions ->
            val extensionSources = extensions
                .filter { it.isEnabled }
                .flatMap { extensionManager.loadSources(it.pkgName) }
            builtInSources + extensionSources
        }
        .stateIn(scope, SharingStarted.Eagerly, builtInSources)

    fun byId(sourceId: String): MediaSource? = sources.value.firstOrNull { it.id == sourceId }

    fun byMediaType(mediaType: MediaType): List<MediaSource> =
        sources.value.filter { it.mediaType == mediaType }
}
