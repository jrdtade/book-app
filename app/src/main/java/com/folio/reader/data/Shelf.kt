package com.folio.reader.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A user-created shelf (e.g. "Sci-Fi", "To re-read") that books can be grouped into. */
@Entity(tableName = "collections")
data class Shelf(
    @PrimaryKey val id: String,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
)

/** Many-to-many join: a book can belong to any number of shelves. */
@Entity(tableName = "book_collections", primaryKeys = ["bookId", "collectionId"])
data class BookCollectionCrossRef(
    val bookId: String,
    val collectionId: String,
)
