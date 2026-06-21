package com.folio.reader.source

import com.folio.reader.data.BookDao
import com.folio.reader.data.LOCAL_COMIC_SOURCE_ID
import com.folio.reader.data.LOCAL_MANGA_SOURCE_ID
import com.folio.reader.data.MediaType

/** Wraps an imported CBZ/CBR archive (already extracted to disk by CbzCbrParser) behind
 *  the MediaSource contract. The whole archive is a single read unit, so it exposes one
 *  chapter whose pages are every extracted page image, in reading order. */
class CbzCbrSource(private val bookDao: BookDao, override val mediaType: MediaType) : MediaSource {
    init { require(mediaType == MediaType.COMIC || mediaType == MediaType.MANGA) }

    override val id = if (mediaType == MediaType.MANGA) LOCAL_MANGA_SOURCE_ID else LOCAL_COMIC_SOURCE_ID
    override val name = if (mediaType == MediaType.MANGA) "Local Manga Archive" else "Local Comic Archive"

    // No remote catalog to poll - everything this source has is already in the library.
    override suspend fun fetchLatestUpdates(): List<SourceMediaInfo> = emptyList()

    override suspend fun fetchMediaDetails(sourceMediaId: String): SourceMediaDetails {
        val book = bookDao.get(sourceMediaId) ?: error("Unknown local archive: $sourceMediaId")
        return SourceMediaDetails(
            sourceMediaId = book.id,
            title = book.title,
            author = book.author,
            description = null,
            coverUrl = book.coverPath,
            genre = book.genre,
        )
    }

    override suspend fun fetchChapterList(sourceMediaId: String): List<SourceChapter> {
        val book = bookDao.get(sourceMediaId) ?: error("Unknown local archive: $sourceMediaId")
        return listOf(SourceChapter(chapterId = sourceMediaId, title = book.title, number = 1f))
    }

    override suspend fun fetchPageList(chapterId: String): List<SourcePage> {
        val book = bookDao.get(chapterId) ?: error("Unknown local archive: $chapterId")
        return pageFiles(book.spine).mapIndexed { index, path -> SourcePage(index, path) }
    }

    private fun pageFiles(spine: String): List<String> = spine.split("\n").filter { it.isNotBlank() }
}
