package com.folio.reader.source

import com.folio.reader.data.MediaType

/** A catalog entry as surfaced by a source, before it's been imported into the local library. */
data class SourceMediaInfo(
    val sourceMediaId: String,
    val title: String,
    val author: String,
    val coverUrl: String?,
)

data class SourceMediaDetails(
    val sourceMediaId: String,
    val title: String,
    val author: String,
    val description: String?,
    val coverUrl: String?,
    val genre: String?,
)

data class SourceChapter(
    val chapterId: String,
    val title: String,
    val number: Float,
)

/** One unit of chapter content. For text sources (EPUB) this is a document path;
 *  for image sources (manga/comic) it's a path/URL to a single page image. */
data class SourcePage(
    val index: Int,
    val contentPath: String,
)

/**
 * Contract for anything that can supply media (an EPUB, a manga, a comic) and its
 * chapters/pages, whether backed by local files or a remote catalog. Mirrors the
 * Tachiyomi/Mihon source model so the rest of the app (library, reader) never needs
 * to know whether a given book came from disk or from a network extension.
 */
interface MediaSource {
    val id: String
    val name: String
    val mediaType: MediaType

    /** Newly available/updated media from this source's catalog, if it has one. */
    suspend fun fetchLatestUpdates(): List<SourceMediaInfo>

    /** Filters this source's catalog supports (e.g. genre, status, sort order),
     *  surfaced to a filter sheet in the browse UI. Empty by default. */
    fun getFilters(): List<SourceFilter> = emptyList()

    /** Searches this source's catalog. The default falls back to client-side
     *  filtering of [fetchLatestUpdates] by title, ignoring [filters] — sources
     *  with a real remote catalog (e.g. [com.folio.reader.extension.TachiyomiSourceAdapter])
     *  override this to hit the actual search/filter endpoint. */
    suspend fun search(query: String, filters: List<SourceFilter> = emptyList()): List<SourceMediaInfo> =
        fetchLatestUpdates().filter { it.title.contains(query, ignoreCase = true) }

    suspend fun fetchMediaDetails(sourceMediaId: String): SourceMediaDetails

    suspend fun fetchChapterList(sourceMediaId: String): List<SourceChapter>

    suspend fun fetchPageList(chapterId: String): List<SourcePage>
}
