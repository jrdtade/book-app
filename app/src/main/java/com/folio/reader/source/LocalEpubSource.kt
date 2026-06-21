package com.folio.reader.source

import com.folio.reader.data.BookDao
import com.folio.reader.data.LOCAL_EPUB_SOURCE_ID
import com.folio.reader.data.MediaType

/** Wraps already-imported EPUBs (parsed by [com.folio.reader.epub.EpubParser] at import
 *  time) behind the [MediaSource] contract. Chapter ids are "<bookId>::<spineIndex>". */
class LocalEpubSource(private val bookDao: BookDao) : MediaSource {
    override val id = LOCAL_EPUB_SOURCE_ID
    override val name = "Local EPUB"
    override val mediaType = MediaType.EPUB

    // No remote catalog to poll - everything this source has is already in the library.
    override suspend fun fetchLatestUpdates(): List<SourceMediaInfo> = emptyList()

    override suspend fun fetchMediaDetails(sourceMediaId: String): SourceMediaDetails {
        val book = bookDao.get(sourceMediaId) ?: error("Unknown local EPUB: $sourceMediaId")
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
        val book = bookDao.get(sourceMediaId) ?: error("Unknown local EPUB: $sourceMediaId")
        return spineFiles(book.spine).mapIndexed { index, _ ->
            SourceChapter(chapterId = chapterId(sourceMediaId, index), title = "Chapter ${index + 1}", number = (index + 1).toFloat())
        }
    }

    override suspend fun fetchPageList(chapterId: String): List<SourcePage> {
        val (bookId, index) = parseChapterId(chapterId)
        val book = bookDao.get(bookId) ?: error("Unknown local EPUB: $bookId")
        val file = spineFiles(book.spine).getOrNull(index) ?: error("Chapter index out of range: $index")
        return listOf(SourcePage(index = 0, contentPath = file))
    }

    private fun spineFiles(spine: String): List<String> = spine.split("\n").filter { it.isNotBlank() }

    private fun chapterId(bookId: String, index: Int) = "$bookId::$index"

    private fun parseChapterId(chapterId: String): Pair<String, Int> {
        val (bookId, indexStr) = chapterId.split("::", limit = 2)
        return bookId to indexStr.toInt()
    }
}
