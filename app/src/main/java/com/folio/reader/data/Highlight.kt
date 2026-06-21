package com.folio.reader.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "highlights")
data class Highlight(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: String,
    val bookTitle: String,
    val author: String,
    val text: String,
    val note: String? = null,
    val chapterIndex: Int = 0,
    val color: String = "y",
    val createdAt: Long = System.currentTimeMillis(),
)

/** A saved spot in a book the user can jump back to. */
@Entity(tableName = "bookmarks")
data class Bookmark(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: String,
    val chapterIndex: Int,
    val fraction: Float,
    val label: String,
    val createdAt: Long = System.currentTimeMillis(),
)

