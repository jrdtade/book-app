package com.folio.reader.extension

import android.content.Context
import androidx.preference.PreferenceManager
import androidx.preference.PreferenceScreen
import com.folio.reader.data.MediaType
import com.folio.reader.source.MediaSource
import com.folio.reader.source.SourceChapter
import com.folio.reader.source.SourceFilter
import com.folio.reader.source.SourceMediaDetails
import com.folio.reader.source.SourceMediaInfo
import com.folio.reader.source.SourcePage
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.ConfigurableSource
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
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

    val isConfigurable: Boolean get() = source is ConfigurableSource

    /** The live [FilterList] handed out by [getFilters] — re-used (not recreated)
     *  by [search] so the state the user set on it actually takes effect, since
     *  some extensions read filter state straight off the same object instance. */
    private var liveFilterList: FilterList? = null

    override suspend fun fetchLatestUpdates(): List<SourceMediaInfo> = withContext(Dispatchers.IO) {
        val catalogue = source as? CatalogueSource ?: return@withContext emptyList()
        val page = if (catalogue.supportsLatest) {
            catalogue.fetchLatestUpdates(1).awaitFirst()
        } else {
            catalogue.fetchPopularManga(1).awaitFirst()
        }
        page.mangas.map { it.toSourceMediaInfo() }
    }

    override fun getFilters(): List<SourceFilter> {
        val catalogue = source as? CatalogueSource ?: return emptyList()
        val list = catalogue.getFilterList()
        liveFilterList = list
        return list.map { it.toSourceFilter() }
    }

    override suspend fun search(query: String, filters: List<SourceFilter>): List<SourceMediaInfo> =
        withContext(Dispatchers.IO) {
            val catalogue = source as? CatalogueSource ?: return@withContext emptyList()
            val realFilters = liveFilterList ?: catalogue.getFilterList().also { liveFilterList = it }
            applyState(realFilters, filters)
            catalogue.fetchSearchManga(1, query, realFilters).awaitFirst().mangas.map { it.toSourceMediaInfo() }
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

    /** Builds the real (Android-View-based) preference hierarchy a [ConfigurableSource]
     *  declares, bound to a SharedPreferences file named the same way Tachiyomi/Mihon
     *  extensions look it up themselves (`source_<id>`), so values persisted here are
     *  the same ones the extension reads at request time. Null if not configurable. */
    fun buildPreferenceScreen(context: Context): PreferenceScreen? {
        val configurable = source as? ConfigurableSource ?: return null
        val manager = PreferenceManager(context)
        manager.sharedPreferencesName = "source_${source.id}"
        val screen = manager.createPreferenceScreen(context)
        configurable.setupPreferenceScreen(screen)
        return screen
    }

    private fun SManga.toSourceMediaInfo() = SourceMediaInfo(
        sourceMediaId = url,
        title = title,
        author = author ?: "Unknown",
        coverUrl = thumbnail_url,
    )

    private fun Filter<*>.toSourceFilter(): SourceFilter = when (this) {
        is Filter.Header -> SourceFilter.Header(name)
        is Filter.Separator -> SourceFilter.Separator(name)
        is Filter.CheckBox -> SourceFilter.CheckBox(name, state)
        is Filter.TriState -> SourceFilter.TriState(name, state)
        is Filter.Text -> SourceFilter.Text(name, state)
        is Filter.Select<*> -> SourceFilter.Select(name, values.map { it.toString() }, state)
        is Filter.Sort -> SourceFilter.Sort(
            name = name,
            options = values.toList(),
            selectedIndex = state?.index,
            ascending = state?.ascending ?: true,
        )
        is Filter.Group<*> -> SourceFilter.Group(
            name = name,
            filters = state.filterIsInstance<Filter<*>>().map { it.toSourceFilter() },
        )
        else -> SourceFilter.Separator(name)
    }

    @Suppress("UNCHECKED_CAST")
    private fun applyState(real: List<Filter<*>>, ours: List<SourceFilter>) {
        real.forEachIndexed { index, filter ->
            val our = ours.getOrNull(index) ?: return@forEachIndexed
            when {
                filter is Filter.CheckBox && our is SourceFilter.CheckBox -> filter.state = our.checked
                filter is Filter.TriState && our is SourceFilter.TriState -> filter.state = our.state
                filter is Filter.Text && our is SourceFilter.Text -> filter.state = our.value
                filter is Filter.Select<*> && our is SourceFilter.Select ->
                    (filter as Filter.Select<Any?>).state = our.selected
                filter is Filter.Sort && our is SourceFilter.Sort ->
                    filter.state = our.selectedIndex?.let { Filter.Sort.Selection(it, our.ascending) }
                filter is Filter.Group<*> && our is SourceFilter.Group ->
                    applyState(filter.state.filterIsInstance<Filter<*>>(), our.filters)
                else -> Unit
            }
        }
    }
}

/** Extensions hand back cold, single-shot Observables for one page of results;
 *  blocking for the first (only) emission is the simplest correct bridge into a
 *  suspend function already running on [Dispatchers.IO]. */
private fun <T> Observable<T>.awaitFirst(): T = toBlocking().first()
