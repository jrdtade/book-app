package com.folio.reader.source

import com.folio.reader.data.MediaType
import com.folio.reader.extension.ExtensionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Holds every known [MediaSource] — installed/enabled extensions only. Local
 *  library content (EPUBs, CBZ/CBR) is read straight from the database elsewhere
 *  and was never really "browsable" through this contract (no remote catalog to
 *  fetch), so it isn't represented here. */
class SourceRegistry(
    private val extensionManager: ExtensionManager,
    scope: CoroutineScope
) {
    /** Reactively rebuilds whenever an extension is discovered, removed, or toggled.
     *  Only enabled extensions get their sources dynamically loaded; disabled ones
     *  are skipped entirely. */
    val sources: StateFlow<List<MediaSource>> = extensionManager.extensions
        .map { extensions ->
            extensions
                .filter { it.isEnabled }
                .flatMap { extensionManager.loadSources(it.pkgName) }
        }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    fun byId(sourceId: String): MediaSource? = sources.value.firstOrNull { it.id == sourceId }

    fun byMediaType(mediaType: MediaType): List<MediaSource> =
        sources.value.filter { it.mediaType == mediaType }
}
