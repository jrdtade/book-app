package com.folio.reader.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.folio.reader.FolioApp
import com.folio.reader.data.Book
import com.folio.reader.data.ReadStatus
import com.folio.reader.data.ReadingMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

class ComicReaderViewModel(private val app: FolioApp) : ViewModel() {
    private val repo = app.repository

    private val _book = MutableStateFlow<Book?>(null)
    val book: StateFlow<Book?> = _book

    private var loadedBookId: String? = null

    fun load(bookId: String) {
        if (loadedBookId == bookId) return
        loadedBookId = bookId
        viewModelScope.launch { _book.value = repo.bookDao.get(bookId) }
    }

    fun pageFiles(): List<File> {
        val b = _book.value ?: return emptyList()
        return b.spine.split("\n").filter { it.isNotBlank() }.map { File(b.contentDir, it) }
    }

    fun updatePage(pageIndex: Int) {
        val b = _book.value ?: return
        val total = b.chapterCount.coerceAtLeast(1)
        val clamped = pageIndex.coerceIn(0, total - 1)
        if (clamped == b.currentChapter) return
        val updated = b.copy(
            currentChapter = clamped,
            chapterProgress = if (total <= 1) 1f else clamped.toFloat() / (total - 1).toFloat(),
            status = if (b.status == ReadStatus.WANT) ReadStatus.READING else b.status,
        )
        _book.value = updated
        viewModelScope.launch { repo.updateBook(updated) }
    }

    fun setReadingMode(mode: ReadingMode) {
        val b = _book.value ?: return
        if (b.readingMode == mode) return
        val updated = b.copy(readingMode = mode)
        _book.value = updated
        viewModelScope.launch { repo.updateBook(updated) }
    }

    fun setFinished(rating: Int) {
        val b = _book.value ?: return
        val updated = b.copy(status = ReadStatus.FINISHED, rating = rating, finishedAt = System.currentTimeMillis())
        _book.value = updated
        viewModelScope.launch { repo.updateBook(updated) }
    }
}
