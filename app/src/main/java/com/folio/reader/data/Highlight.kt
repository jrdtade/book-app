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
    val color: String = "y",
    val createdAt: Long = System.currentTimeMillis(),
)
