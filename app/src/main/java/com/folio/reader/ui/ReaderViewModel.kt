package com.folio.reader.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.folio.reader.FolioApp
import com.folio.reader.data.Book
import com.folio.reader.data.Bookmark
import com.folio.reader.data.Highlight
import com.folio.reader.data.ReadStatus
import com.folio.reader.data.ReaderPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.ceil

data class SearchHit(val chapterIndex: Int, val snippet: String, val charOffset: Int)

/** Whole-book page number (1-based) and total, independent of per-chapter pagination. */
data class GlobalPage(val page: Int, val total: Int)

private const val CHARS_PER_PAGE = 1600

@OptIn(ExperimentalCoroutinesApi::class)
class ReaderViewModel(private val app: FolioApp) : ViewModel() {
    private val repo = app.repository
    private val prefsRepo = app.readerPrefsRepository
    private val dayFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    val prefs: StateFlow<ReaderPrefs> = prefsRepo.prefsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, ReaderPrefs())

    private val _book = MutableStateFlow<Book?>(null)
    val book: StateFlow<Book?> = _book

    val highlights: StateFlow<List<Highlight>> = _book.flatMapLatest { b ->
        if (b == null) flowOf(emptyList()) else repo.observeHighlightsForBook(b.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookmarks: StateFlow<List<Bookmark>> = _book.flatMapLatest { b ->
        if (b == null) flowOf(emptyList()) else repo.observeBookmarksForBook(b.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchResults = MutableStateFlow<List<SearchHit>>(emptyList())
    val searchResults: StateFlow<List<SearchHit>> = _searchResults

    private val _searching = MutableStateFlow(false)
    val searching: StateFlow<Boolean> = _searching

    private val _chapterCharCounts = MutableStateFlow<List<Int>>(emptyList())

    /** Estimated whole-book page number, e.g. "Page 1 of 500", stable regardless of font/size changes. */
    val globalPage: StateFlow<GlobalPage> = combine(_book, _chapterCharCounts) { b, counts ->
        if (b == null || counts.isEmpty()) return@combine GlobalPage(1, 1)
        val totalChars = counts.sum().coerceAtLeast(1)
        val charsBefore = counts.take(b.currentChapter).sum()
        val charsInChapter = counts.getOrElse(b.currentChapter) { 0 }
        val charsIn = charsBefore + (b.chapterProgress * charsInChapter)
        val total = ceil(totalChars / CHARS_PER_PAGE.toDouble()).toInt().coerceAtLeast(1)
        val page = (charsIn / CHARS_PER_PAGE).toInt().coerceIn(0, total - 1) + 1
        GlobalPage(page, total)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GlobalPage(1, 1))

    private var sessionStartMs = 0L
    private var loadedBookId: String? = null

    fun load(bookId: String) {
        if (loadedBookId == bookId) return
        loadedBookId = bookId
        viewModelScope.launch {
            val loaded = repo.bookDao.get(bookId)
            _book.value = loaded
            sessionStartMs = System.currentTimeMillis()
            if (loaded != null) {
                _chapterCharCounts.value = withContext(Dispatchers.IO) {
                    chapterPaths().map { rel ->
                        val file = File(loaded.contentDir, rel)
                        if (!file.exists()) return@map 0
                        Regex("<[^>]+>").replace(file.readText(Charsets.UTF_8), "").trim().length
                    }
                }
            }
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

    fun addHighlight(text: String, note: String? = null) {
        val b = _book.value ?: return
        viewModelScope.launch {
            repo.addHighlight(
                Highlight(bookId = b.id, bookTitle = b.title, author = b.author, text = text, note = note, chapterIndex = b.currentChapter),
            )
        }
    }

    fun deleteHighlight(highlight: Highlight) {
        viewModelScope.launch { repo.deleteHighlight(highlight) }
    }

    fun addBookmark() {
        val b = _book.value ?: return
        val label = "Chapter ${b.currentChapter + 1}"
        viewModelScope.launch {
            repo.addBookmark(Bookmark(bookId = b.id, chapterIndex = b.currentChapter, fraction = b.chapterProgress, label = label))
        }
    }

    fun deleteBookmark(bookmark: Bookmark) {
        viewModelScope.launch { repo.deleteBookmark(bookmark) }
    }

    /** Full-text search across every chapter's plain text. Chapter-granularity jump target. */
    fun search(query: String) {
        val b = _book.value
        if (query.isBlank() || b == null) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            _searching.value = true
            _searchResults.value = withContext(Dispatchers.IO) {
                val paths = chapterPaths()
                val hits = mutableListOf<SearchHit>()
                paths.forEachIndexed { index, relPath ->
                    val file = File(b.contentDir, relPath)
                    if (!file.exists()) return@forEachIndexed
                    val text = Regex("<[^>]+>").replace(file.readText(Charsets.UTF_8), " ")
                        .replace(Regex("\\s+"), " ")
                    var from = 0
                    while (true) {
                        val at = text.indexOf(query, from, ignoreCase = true)
                        if (at < 0) break
                        val start = (at - 40).coerceAtLeast(0)
                        val end = (at + query.length + 40).coerceAtMost(text.length)
                        hits.add(SearchHit(index, "…${text.substring(start, end).trim()}…", at))
                        from = at + query.length
                        if (hits.size >= 200) return@withContext hits
                    }
                }
                hits
            }
            _searching.value = false
        }
    }

    fun clearSearch() {
        _searchResults.value = emptyList()
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
