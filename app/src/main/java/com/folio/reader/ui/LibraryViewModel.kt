package com.folio.reader.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.folio.reader.FolioApp
import com.folio.reader.data.Book
import com.folio.reader.data.LOCAL_COMIC_SOURCE_ID
import com.folio.reader.data.LOCAL_MANGA_SOURCE_ID
import com.folio.reader.data.MediaType
import com.folio.reader.data.ReadingMode
import com.folio.reader.data.Shelf
import com.folio.reader.data.ReadStatus
import com.folio.reader.data.ReadingSession
import com.folio.reader.network.BookRecommendation
import com.folio.reader.network.CoverCandidate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class LibraryViewModel(private val app: FolioApp) : ViewModel() {
    private val repo = app.repository

    val books: StateFlow<List<Book>> = repo.observeBooks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Books of a single category (eBooks/Manga/Comics/Light Novels), filtered at the
     *  Room query level rather than client-side so the Library screen's tabs are each
     *  backed by their own [com.folio.reader.data.BookDao.observeByType] flow. */
    fun observeByType(mediaType: MediaType): Flow<List<Book>> = repo.observeBooksByType(mediaType)

    val sessions: StateFlow<List<ReadingSession>> = repo.observeSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _recommendation = MutableStateFlow<BookRecommendation?>(null)
    val recommendation: StateFlow<BookRecommendation?> = _recommendation.asStateFlow()

    private val _isRecommending = MutableStateFlow(false)
    val isRecommending: StateFlow<Boolean> = _isRecommending.asStateFlow()

    fun refreshRecommendation() {
        if (_isRecommending.value) return
        viewModelScope.launch {
            _isRecommending.value = true
            _recommendation.value = repo.getRecommendation(books.value, sessions.value)
            _isRecommending.value = false
        }
    }

    val collections: StateFlow<List<Shelf>> = repo.observeCollections()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun importEpub(uri: Uri, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            val book = runCatching { repo.importEpub(uri) }.getOrNull()
            onComplete?.invoke()
            if (book != null) viewModelScope.launch { runCatching { repo.classifyBook(book) } }
        }
    }

    fun importComic(uri: Uri, displayName: String?, mediaType: MediaType, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            runCatching { repo.importComic(uri, displayName, mediaType) }
            onComplete?.invoke()
        }
    }

    fun setStatus(book: Book, status: ReadStatus) {
        viewModelScope.launch { repo.updateBook(book.copy(status = status)) }
    }

    fun setRating(book: Book, rating: Int) {
        viewModelScope.launch { repo.updateBook(book.copy(rating = rating)) }
    }

    fun deleteBook(book: Book) {
        viewModelScope.launch { repo.deleteBook(book) }
    }

    /** Moves a comic archive import between the Comics and Manga categories - the
     *  importer can't tell them apart from the file alone, so this is a manual call. */
    fun recategorize(book: Book, mediaType: MediaType) {
        if (book.mediaType != MediaType.COMIC && book.mediaType != MediaType.MANGA) return
        if (mediaType != MediaType.COMIC && mediaType != MediaType.MANGA) return
        viewModelScope.launch {
            repo.updateBook(
                book.copy(
                    mediaType = mediaType,
                    sourceId = if (mediaType == MediaType.MANGA) LOCAL_MANGA_SOURCE_ID else LOCAL_COMIC_SOURCE_ID,
                    readingMode = if (mediaType == MediaType.MANGA) ReadingMode.PAGED_RTL else ReadingMode.PAGED_LTR,
                ),
            )
        }
    }

    private val _coverResults = MutableStateFlow<List<CoverCandidate>>(emptyList())
    val coverResults: StateFlow<List<CoverCandidate>> = _coverResults.asStateFlow()

    private val _coverSearchInProgress = MutableStateFlow(false)
    val coverSearchInProgress: StateFlow<Boolean> = _coverSearchInProgress.asStateFlow()

    fun searchCovers(query: String) {
        viewModelScope.launch {
            _coverSearchInProgress.value = true
            _coverResults.value = runCatching { repo.searchCoverCandidates(query) }.getOrDefault(emptyList())
            _coverSearchInProgress.value = false
        }
    }

    fun clearCoverResults() {
        _coverResults.value = emptyList()
    }

    fun applyCover(book: Book, imageUrl: String, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            runCatching { repo.applyCover(book, imageUrl) }
            onComplete?.invoke()
        }
    }

    fun createCollection(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { repo.createCollection(name.trim()) }
    }

    fun deleteCollection(shelf: Shelf) {
        viewModelScope.launch { repo.deleteCollection(shelf) }
    }

    fun collectionIdsForBook(bookId: String) = repo.observeCollectionIdsForBook(bookId)

    fun bookIdsInCollection(collectionId: String) = repo.observeBookIdsInCollection(collectionId)

    fun toggleBookInCollection(bookId: String, collectionId: String, inCollection: Boolean) {
        viewModelScope.launch {
            if (inCollection) repo.removeBookFromCollection(bookId, collectionId)
            else repo.addBookToCollection(bookId, collectionId)
        }
    }
}
