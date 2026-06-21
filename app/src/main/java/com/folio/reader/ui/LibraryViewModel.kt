package com.folio.reader.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.folio.reader.FolioApp
import com.folio.reader.data.Book
import com.folio.reader.data.BookCollection
import com.folio.reader.data.ReadStatus
import com.folio.reader.data.RemoteBookInfo
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class LibraryViewModel(private val app: FolioApp) : ViewModel() {
    private val repo = app.repository

    val books: StateFlow<List<Book>> = repo.observeBooks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val collections: StateFlow<List<BookCollection>> = repo.observeCollections()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createCollection(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repo.addCollection(BookCollection(id = UUID.randomUUID().toString(), name = name.trim()))
        }
    }

    fun deleteCollection(collection: BookCollection) {
        viewModelScope.launch { repo.deleteCollection(collection) }
    }

    fun assignToCollection(book: Book, collectionId: String?) {
        viewModelScope.launch { repo.updateBook(book.copy(collectionId = collectionId)) }
    }

    fun importEpub(uri: Uri, onImported: (Book) -> Unit = {}) {
        viewModelScope.launch {
            runCatching { repo.importEpub(uri) }.onSuccess { onImported(it) }
        }
    }

    fun updateBook(book: Book) {
        viewModelScope.launch { repo.updateBook(book) }
    }

    fun fetchSynopsis(book: Book) {
        viewModelScope.launch {
            val synopsis = runCatching { RemoteBookInfo.fetchSynopsis(book.title, book.author) }.getOrNull()
            if (!synopsis.isNullOrBlank()) repo.updateBook(book.copy(synopsis = synopsis))
        }
    }

    fun setCoverFromUrl(book: Book, imageUrl: String) {
        viewModelScope.launch {
            val path = runCatching { RemoteBookInfo.downloadCover(book.contentDir, imageUrl) }.getOrNull()
            if (path != null) repo.updateBook(book.copy(coverPath = path))
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
}
