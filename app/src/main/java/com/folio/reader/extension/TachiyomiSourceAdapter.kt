package com.folio.reader.extension

import com.folio.reader.data.MediaType
import com.folio.reader.source.MediaSource
import com.folio.reader.source.SourceChapter
import com.folio.reader.source.SourceMediaDetails
import com.folio.reader.source.SourceMediaInfo
import com.folio.reader.source.SourcePage
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rx.Observable

/**
 * Bridges a real, dynamically-loaded Mihon/Keiyoushi [Source] into this app's
 * [MediaSource] contract. Extensions speak in [SManga]/[SChapter]/cold RxJava 1
 * [Observable]s; we re-key everything off `url` (the same field Tachiyomi itself
 * uses as a manga/chapter's stable identity) and block onto IO dispatchers to
 * turn the Observable into the single value our suspend-fun contract expects.
 */
class TachiyomiSourceAdapter(private val source: Source) : MediaSource {
    override val id: String = "tachi_${source.id}"
    override val name: String = source.name
    override val mediaType: MediaType = MediaType.MANGA

    override suspend fun fetchLatestUpdates(): List<SourceMediaInfo> = withContext(Dispatchers.IO) {
        val catalogue = source as? CatalogueSource ?: return@withContext emptyList()
        val page = if (catalogue.supportsLatest) {
            catalogue.fetchLatestUpdates(1).awaitFirst()
        } else {
            catalogue.fetchPopularManga(1).awaitFirst()
        }
        page.mangas.map { it.toSourceMediaInfo() }
    }

    override suspend fun fetchMediaDetails(sourceMediaId: String): SourceMediaDetails = withContext(Dispatchers.IO) {
        val manga = SManga.create().apply { url = sourceMediaId }
        val details = source.fetchMangaDetails(manga).awaitFirst()
        SourceMediaDetails(
            sourceMediaId = sourceMediaId,
            title = details.title,
            author = details.author ?: "Unknown",
            description = details.description,
            coverUrl = details.thumbnail_url,
            genre = details.genre,
        )
    }

    override suspend fun fetchChapterList(sourceMediaId: String): List<SourceChapter> = withContext(Dispatchers.IO) {
        val manga = SManga.create().apply { url = sourceMediaId }
        source.fetchChapterList(manga).awaitFirst().map {
            SourceChapter(chapterId = it.url, title = it.name, number = it.chapter_number)
        }
    }

    override suspend fun fetchPageList(chapterId: String): List<SourcePage> = withContext(Dispatchers.IO) {
        val chapter = SChapter.create().apply { url = chapterId }
        source.fetchPageList(chapter).awaitFirst().mapIndexed { index, page ->
            SourcePage(index = index, contentPath = page.imageUrl ?: page.url)
        }
    }

    private fun SManga.toSourceMediaInfo() = SourceMediaInfo(
        sourceMediaId = url,
        title = title,
        author = author ?: "Unknown",
        coverUrl = thumbnail_url,
    )
}

/** Extensions hand back cold, single-shot Observables for one page of results;
 *  blocking for the first (only) emission is the simplest correct bridge into a
 *  suspend function already running on [Dispatchers.IO]. */
private fun <T> Observable<T>.awaitFirst(): T = toBlocking().first()
