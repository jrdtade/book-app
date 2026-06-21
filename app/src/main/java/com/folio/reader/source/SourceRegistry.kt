package com.folio.reader.source

import com.folio.reader.data.BookDao
import com.folio.reader.data.MediaType

/** Holds every known [MediaSource], local or remote, keyed for lookup by the
 *  library/reader UI without those layers needing to know concrete source types. */
class SourceRegistry(bookDao: BookDao) {
    private val sources: List<MediaSource> = listOf(
        LocalEpubSource(bookDao),
        CbzCbrSource(bookDao, MediaType.COMIC),
        CbzCbrSource(bookDao, MediaType.MANGA),
        StubNetworkSource(id = "stub_manga_network", name = "Network (stub)", mediaType = MediaType.MANGA),
    )

    fun byId(sourceId: String): MediaSource? = sources.firstOrNull { it.id == sourceId }

    fun byMediaType(mediaType: MediaType): List<MediaSource> = sources.filter { it.mediaType == mediaType }
}
