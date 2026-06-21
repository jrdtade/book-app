package com.folio.reader.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ReadStatus { WANT, READING, FINISHED }

@Entity(tableName = "books")
data class Book(
    @PrimaryKey val id: String,
    val title: String,
    val author: String,
    /** Directory (under filesDir/books/<id>) holding the unpacked EPUB contents. */
    val contentDir: String,
    val coverPath: String?,
    /** Absolute spine order of chapter file paths, joined with '\n'. */
    val spine: String,
    val chapterCount: Int,
    var currentChapter: Int = 0,
    /** 0f..1f progress within the current chapter (paged mode: page/total). */
    var chapterProgress: Float = 0f,
    var status: ReadStatus = ReadStatus.WANT,
    var rating: Int = 0,
    var addedAt: Long = System.currentTimeMillis(),
    var finishedAt: Long? = null,
    var totalReadMillis: Long = 0,
    /** Cover background as a hex string so the typographic cover can render without the file. */
    var coverColorA: Long = 0xFF3A4EA8,
    var coverColorB: Long = 0xFF26336E,
    /** Synopsis fetched from the Google Books API, cached locally once found. */
    var synopsis: String? = null,
    var synopsisFetchFailed: Boolean = false,
    var publishedDate: String? = null,
    /** Genre and descriptive tags classified by Gemini on import, cached locally once found. */
    var genre: String? = null,
    /** Comma-separated descriptive tags. */
    var tags: String? = null,
    var classificationFetchFailed: Boolean = false,
)

fun Book.overallProgress(): Float =
    if (chapterCount <= 0) 0f
    else ((currentChapter + chapterProgress) / chapterCount).coerceIn(0f, 1f)
