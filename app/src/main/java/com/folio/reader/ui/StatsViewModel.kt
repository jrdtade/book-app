package com.folio.reader.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.folio.reader.FolioApp
import com.folio.reader.data.Book
import com.folio.reader.data.Highlight
import com.folio.reader.data.ReadStatus
import com.folio.reader.data.ReadingSession
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class StatsState(
    val booksRead: Int = 0,
    val goal: Int = 24,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val hoursThisWeek: Float = 0f,
    val weekMinutes: List<Int> = listOf(0, 0, 0, 0, 0, 0, 0),
    val heat: List<Int> = List(18 * 7) { 0 },
    val pagesYear: Int = 0,
    val hoursYear: Float = 0f,
    val genreBreakdown: List<Pair<String, Int>> = emptyList(),
    val topAuthors: List<Pair<String, Int>> = emptyList(),
    val highlights: List<Highlight> = emptyList(),
)

class StatsViewModel(app: FolioApp) : ViewModel() {
    private val repo = app.repository

    val state: StateFlow<StatsState> = combine(
        repo.observeBooks(), repo.observeSessions(), repo.observeHighlights(),
    ) { books, sessions, highlights ->
        computeStats(books, sessions, highlights)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatsState())

    private val dayFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private fun computeStats(books: List<Book>, sessions: List<ReadingSession>, highlights: List<Highlight>): StatsState {
        val finished = books.count { it.status == ReadStatus.FINISHED }
        val minutesByDay = sessions.groupBy { it.day }.mapValues { (_, s) -> s.sumOf { it.durationMillis } / 60000 }

        // streaks
        var streak = 0
        var longest = 0
        var running = 0
        // walk back from today
        val today = Calendar.getInstance()
        var probe = today.clone() as Calendar
        var counting = true
        for (i in 0 until 365) {
            val key = dayFmt.format(probe.time)
            val read = (minutesByDay[key] ?: 0L) > 0
            if (read) { running++; longest = maxOf(longest, running) } else { running = 0 }
            if (counting) {
                if (read) streak++ else if (i > 0) counting = false
            }
            probe.add(Calendar.DAY_OF_YEAR, -1)
        }

        val weekMinutes = (6 downTo 0).map { offset ->
            val c = today.clone() as Calendar
            c.add(Calendar.DAY_OF_YEAR, -offset)
            (minutesByDay[dayFmt.format(c.time)] ?: 0L).toInt()
        }
        val hoursThisWeek = weekMinutes.sum() / 60f

        val heat = (125 downTo 0).map { offset ->
            val c = today.clone() as Calendar
            c.add(Calendar.DAY_OF_YEAR, -offset)
            val mins = minutesByDay[dayFmt.format(c.time)] ?: 0L
            when {
                mins <= 0 -> 0
                mins < 15 -> 1
                mins < 30 -> 2
                mins < 60 -> 3
                else -> 4
            }
        }

        val totalMinutesYear = sessions.sumOf { it.durationMillis } / 60000f
        val pagesYear = (totalMinutesYear / 0.7f).toInt() // rough pages-per-minute estimate

        val genreBreakdown = books.groupBy { it.author }.entries
            .sortedByDescending { it.value.size }
            .take(5)
            .map { it.key to it.value.size }

        val topAuthors = books.filter { it.status == ReadStatus.FINISHED }
            .groupBy { it.author }
            .entries.sortedByDescending { it.value.size }
            .take(3)
            .map { it.key to it.value.size }

        return StatsState(
            booksRead = finished,
            goal = maxOf(24, finished),
            currentStreak = streak,
            longestStreak = longest,
            hoursThisWeek = hoursThisWeek,
            weekMinutes = weekMinutes,
            heat = heat,
            pagesYear = pagesYear,
            hoursYear = totalMinutesYear / 60f,
            genreBreakdown = genreBreakdown,
            topAuthors = topAuthors,
            highlights = highlights,
        )
    }
}
