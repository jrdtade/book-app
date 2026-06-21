package com.folio.reader.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ReadStatus { WANT, READING, FINISHED }

/** Source id for books imported via the local EPUB file parser (as opposed to an extension/source). */
const val LOCAL_EPUB_SOURCE_ID = "local_epub"

/** Source ids for comic/manga archives imported via the local CBZ/CBR file parser. */
const val LOCAL_COMIC_SOURCE_ID = "local_comic"
const val LOCAL_MANGA_SOURCE_ID = "local_manga"

enum class ReadingMode { PAGED_LTR, PAGED_RTL, WEBTOON }

@Entity(tableName = "books")
data class Book(
    @PrimaryKey val id: String,
    val title: String,
    val author: String,
    val mediaType: MediaType = MediaType.EPUB,
    /** Identifier of the source (local parser or extension) this item came from. */
    val sourceId: String = LOCAL_EPUB_SOURCE_ID,
    /** Directory (under filesDir/books/<id>) holding the unpacked content. */
    val contentDir: String,
    val coverPath: String?,
    /** Absolute spine order of chapter (EPUB) or page image (comic/manga) file paths, joined with '\n'. */
    val spine: String,
    val chapterCount: Int,
    var currentChapter: Int = 0,
    /** 0f..1f progress within the current chapter (paged mode: page/total). */
    var chapterProgress: Float = 0f,
    /** Page layout/navigation style for comic/manga readers. Unused for EPUB. */
    var readingMode: ReadingMode = ReadingMode.PAGED_LTR,
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
