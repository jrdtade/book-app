package com.folio.reader.data

import android.content.Context
import android.net.Uri
import com.folio.reader.epub.EpubParser
import kotlinx.coroutines.flow.Flow
import java.io.File

class BookRepository(private val context: Context) {
    private val db = FolioDatabase.get(context)
    val bookDao = db.bookDao()
    val highlightDao = db.highlightDao()
    val sessionDao = db.sessionDao()

    fun observeBooks(): Flow<List<Book>> = bookDao.observeAll()
    fun observeBook(id: String): Flow<Book?> = bookDao.observe(id)
    fun observeHighlights(): Flow<List<Highlight>> = highlightDao.observeAll()
    fun observeSessions(): Flow<List<ReadingSession>> = sessionDao.observeAll()

    suspend fun importEpub(uri: Uri): Book {
        val parsed = EpubParser.import(context, uri)
        val palette = COVER_PALETTES[Math.abs(parsed.title.hashCode()) % COVER_PALETTES.size]
        val book = Book(
            id = parsed.id,
            title = parsed.title,
            author = parsed.author,
            contentDir = parsed.contentDir.absolutePath,
            coverPath = parsed.coverPath,
            spine = parsed.spine.joinToString("\n"),
            chapterCount = parsed.spine.size.coerceAtLeast(1),
            status = ReadStatus.WANT,
            coverColorA = palette.first,
            coverColorB = palette.second,
        )
        bookDao.upsert(book)
        return book
    }

    suspend fun updateBook(book: Book) = bookDao.update(book)

    suspend fun deleteBook(book: Book) {
        bookDao.delete(book)
        File(book.contentDir).deleteRecursively()
    }

    suspend fun recordSession(bookId: String, durationMillis: Long, day: String) {
        if (durationMillis <= 0) return
        sessionDao.insert(ReadingSession(bookId = bookId, startedAt = System.currentTimeMillis(), durationMillis = durationMillis, day = day))
    }

    suspend fun addHighlight(highlight: Highlight) = highlightDao.insert(highlight)

    companion object {
        // long ARGB pairs used for the typographic cover gradient.
        val COVER_PALETTES = listOf(
            0xFF5A6E9CL to 0xFF44537AL,
            0xFF3C4F7CL to 0xFF2D3C60L,
            0xFF4D6E94L to 0xFF374F6EL,
            0xFF6E5C45L to 0xFF52432FL,
            0xFF4D6E62L to 0xFF374F47L,
            0xFF8C5A52L to 0xFF6B3E38L,
            0xFF5A7A8CL to 0xFF3E5866L,
        )
    }
}
