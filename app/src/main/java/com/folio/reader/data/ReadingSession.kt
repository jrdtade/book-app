package com.folio.reader.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** One reading session, used to drive the Stats screen (streaks, heatmap, weekly minutes). */
@Entity(tableName = "sessions")
data class ReadingSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: String,
    val startedAt: Long,
    val durationMillis: Long,
    /** Local-date string yyyy-MM-dd, for day bucketing without timezone surprises. */
    val day: String,
)
