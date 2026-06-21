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

    /** Reactively update the list of available sources based on which extensions are enabled. */
    val sources: StateFlow<List<MediaSource>> = extensionManager.extensions.map { extensions ->
        builtInSources + extensions.filter { it.isEnabled }.mapNotNull { extensionManager.loadSource(it) }
    }.stateIn(scope, SharingStarted.Eagerly, builtInSources)

    fun byId(sourceId: String): MediaSource? = sources.value.firstOrNull { it.id == sourceId }

    fun byMediaType(mediaType: MediaType): List<MediaSource> =
        sources.value.filter { it.mediaType == mediaType }
}
