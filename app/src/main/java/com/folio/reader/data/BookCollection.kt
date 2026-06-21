package com.folio.reader.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A user-defined shelf/collection that books can be filed under. */
@Entity(tableName = "collections")
data class BookCollection(
    @PrimaryKey val id: String,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
)
