package com.folio.reader.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.folio.reader.FolioApp
import com.folio.reader.data.Book
import com.folio.reader.data.ReadStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LibraryViewModel(private val app: FolioApp) : ViewModel() {
    private val repo = app.repository

    val books: StateFlow<List<Book>> = repo.observeBooks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun importEpub(uri: Uri) {
        viewModelScope.launch {
            runCatching { repo.importEpub(uri) }
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
