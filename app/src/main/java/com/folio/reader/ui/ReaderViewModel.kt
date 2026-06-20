package com.folio.reader.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.folio.reader.FolioApp
import com.folio.reader.data.Book
import com.folio.reader.data.Highlight
import com.folio.reader.data.ReadStatus
import com.folio.reader.data.ReaderPrefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class ReaderViewModel(private val app: FolioApp) : ViewModel() {
    private val repo = app.repository
    private val prefsRepo = app.readerPrefsRepository
    private val dayFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    val prefs: StateFlow<ReaderPrefs> = prefsRepo.prefsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, ReaderPrefs())

    private val _book = MutableStateFlow<Book?>(null)
    val book: StateFlow<Book?> = _book

    private var sessionStartMs = 0L
    private var loadedBookId: String? = null

    fun load(bookId: String) {
        if (loadedBookId == bookId) return
        loadedBookId = bookId
        viewModelScope.launch {
            _book.value = repo.bookDao.get(bookId)
            sessionStartMs = System.currentTimeMillis()
        }
    }

    fun chapterPaths(): List<String> = _book.value?.spine?.split("\n")?.filter { it.isNotBlank() } ?: emptyList()

    suspend fun loadChapterHtml(index: Int): String {
        val b = _book.value ?: return ""
        val paths = chapterPaths()
        if (index < 0 || index >= paths.size) return ""
        val relPath = paths[index]
        val file = File(b.contentDir, relPath)
        if (!file.exists()) return ""
        val raw = file.readText(Charsets.UTF_8)
        val body = Regex("<body[^>]*>([\\s\\S]*)</body>", RegexOption.IGNORE_CASE)
            .find(raw)?.groupValues?.get(1) ?: raw
        val baseDir = file.parentFile ?: File(b.contentDir)
        return rewriteAssetUrls(body, baseDir)
    }

    private fun rewriteAssetUrls(html: String, baseDir: File): String {
        return Regex("(src|href)=\"(?!http|data:|#)([^\"]+)\"").replace(html) { m ->
            val attr = m.groupValues[1]
            val ref = m.groupValues[2]
            val resolved = File(baseDir, ref).normalize()
            "$attr=\"file://${resolved.absolutePath}\""
        }
    }

    fun goToChapter(index: Int) {
        val b = _book.value ?: return
        val clamped = index.coerceIn(0, (b.chapterCount - 1).coerceAtLeast(0))
        val updated = b.copy(currentChapter = clamped, chapterProgress = 0f, status = if (b.status == ReadStatus.WANT) ReadStatus.READING else b.status)
        _book.value = updated
        viewModelScope.launch { repo.updateBook(updated) }
    }

    fun updatePageProgress(pageIndex: Int, totalPages: Int) {
        val b = _book.value ?: return
        val pct = if (totalPages <= 1) 0f else pageIndex.toFloat() / (totalPages - 1).toFloat()
        val updated = b.copy(chapterProgress = pct, status = if (b.status == ReadStatus.WANT) ReadStatus.READING else b.status)
        _book.value = updated
        viewModelScope.launch { repo.updateBook(updated) }
    }

    fun setFinished(rating: Int) {
        val b = _book.value ?: return
        val updated = b.copy(status = ReadStatus.FINISHED, rating = rating, finishedAt = System.currentTimeMillis())
        _book.value = updated
        viewModelScope.launch { repo.updateBook(updated) }
    }

    fun addHighlight(text: String) {
        val b = _book.value ?: return
        viewModelScope.launch {
            repo.addHighlight(
                Highlight(bookId = b.id, bookTitle = b.title, author = b.author, text = text),
            )
        }
    }

    fun updatePrefs(transform: (ReaderPrefs) -> ReaderPrefs) {
        viewModelScope.launch { prefsRepo.update(transform(prefs.value)) }
    }

    fun pauseSession() {
        val b = _book.value ?: return
        val elapsed = System.currentTimeMillis() - sessionStartMs
        sessionStartMs = System.currentTimeMillis()
        if (elapsed <= 0) return
        viewModelScope.launch { repo.recordSession(b.id, elapsed, dayFmt.format(java.util.Date())) }
    }
}
