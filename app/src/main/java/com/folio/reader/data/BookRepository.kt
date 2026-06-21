package com.folio.reader.data

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import com.folio.reader.epub.EpubParser
import com.folio.reader.network.CoverCandidate
import com.folio.reader.network.GeminiApi
import com.folio.reader.network.OpenLibraryApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

class BookRepository(private val context: Context) {
    private val db = FolioDatabase.get(context)
    val bookDao = db.bookDao()
    val highlightDao = db.highlightDao()
    val bookmarkDao = db.bookmarkDao()
    val sessionDao = db.sessionDao()
    val collectionDao = db.collectionDao()

    fun observeBooks(): Flow<List<Book>> = bookDao.observeAll()
    fun observeBook(id: String): Flow<Book?> = bookDao.observe(id)
    fun observeHighlights(): Flow<List<Highlight>> = highlightDao.observeAll()
    fun observeHighlightsForBook(bookId: String): Flow<List<Highlight>> = highlightDao.observeForBook(bookId)
    fun observeBookmarksForBook(bookId: String): Flow<List<Bookmark>> = bookmarkDao.observeForBook(bookId)
    fun observeSessions(): Flow<List<ReadingSession>> = sessionDao.observeAll()
    fun observeCollections(): Flow<List<Shelf>> = collectionDao.observeAll()
    fun observeCollectionIdsForBook(bookId: String): Flow<List<String>> = collectionDao.observeCollectionIdsForBook(bookId)
    fun observeBookIdsInCollection(collectionId: String): Flow<List<String>> = collectionDao.observeBookIdsInCollection(collectionId)

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
    suspend fun deleteHighlight(highlight: Highlight) = highlightDao.delete(highlight)

    suspend fun addBookmark(bookmark: Bookmark) = bookmarkDao.insert(bookmark)
    suspend fun deleteBookmark(bookmark: Bookmark) = bookmarkDao.delete(bookmark)

    suspend fun fetchSynopsis(book: Book) {
        val result = GeminiApi.classify(book.title, book.author)
        bookDao.update(
            book.copy(
                synopsis = result?.synopsis ?: book.synopsis,
                synopsisFetchFailed = result?.synopsis == null,
                publishedDate = result?.publishedDate ?: book.publishedDate,
                genre = result?.genre ?: book.genre,
                tags = result?.tags?.joinToString(",") ?: book.tags,
                classificationFetchFailed = result == null,
            ),
        )
    }

    suspend fun classifyBook(book: Book) {
        val result = GeminiApi.classify(book.title, book.author)
        bookDao.update(
            book.copy(
                genre = result?.genre ?: book.genre,
                tags = result?.tags?.joinToString(",") ?: book.tags,
                synopsis = result?.synopsis ?: book.synopsis,
                synopsisFetchFailed = result?.synopsis == null && book.synopsis == null,
                publishedDate = result?.publishedDate ?: book.publishedDate,
                classificationFetchFailed = result == null,
            ),
        )
    }

    suspend fun searchCoverCandidates(query: String): List<CoverCandidate> = OpenLibraryApi.searchCovers(query)

    /** Downloads the chosen cover image into the book's own content directory and
     *  points the book at it, replacing whatever cover (or typographic fallback) it had. */
    suspend fun applyCover(book: Book, imageUrl: String) = withContext(Dispatchers.IO) {
        val bytes = downloadBytes(imageUrl) ?: return@withContext
        val fileName = "cover_${UUID.randomUUID()}.jpg"
        val file = File(book.contentDir, fileName)
        file.writeBytes(bytes)
        bookDao.update(book.copy(coverPath = fileName))
    }

    private fun downloadBytes(imageUrl: String): ByteArray? {
        val connection = URL(imageUrl).openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
            val bytes = connection.inputStream.use { it.readBytes() }
            // Make sure we actually got a decodable image before committing to it.
            if (BitmapFactory.decodeByteArray(bytes, 0, bytes.size) == null) null else bytes
        } catch (_: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }

    suspend fun createCollection(name: String): Shelf {
        val shelf = Shelf(id = UUID.randomUUID().toString(), name = name)
        collectionDao.insert(shelf)
        return shelf
    }

    suspend fun deleteCollection(shelf: Shelf) {
        collectionDao.clearMembers(shelf.id)
        collectionDao.delete(shelf)
    }

    suspend fun addBookToCollection(bookId: String, collectionId: String) =
        collectionDao.addBook(BookCollectionCrossRef(bookId, collectionId))

    suspend fun removeBookFromCollection(bookId: String, collectionId: String) =
        collectionDao.removeBook(bookId, collectionId)

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
