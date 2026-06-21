package eu.kanade.tachiyomi.source.online

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.asObservableSuccess
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import rx.Observable
import uy.kohesive.injekt.api.injectLazy
import java.security.MessageDigest

/**
 * Real implementation of the abstract class most scraping extensions extend.
 * Mirrors Mihon's actual `HttpSource` (not the throwing stub in the published
 * `extensions-lib`): concrete `fetchXxx` methods drive an OkHttp call through the
 * matching `xxxRequest`/`xxxParse` pair, which is what subclasses override.
 */
@Suppress("unused")
abstract class HttpSource : CatalogueSource {

    abstract val baseUrl: String

    open val versionId = 1

    override val id: Long by lazy {
        val key = "${name.lowercase()}/$lang/$versionId"
        val bytes = MessageDigest.getInstance("MD5").digest(key.toByteArray())
        (0..7).map { bytes[it].toLong() and 0xff shl 8 * (7 - it) }.reduce(Long::or) and Long.MAX_VALUE
    }

    /** Real Tachiyomi/Mihon's HttpSource exposes this directly — many extensions
     *  (anything behind Cloudflare) call `network.cloudflareClient` themselves
     *  instead of going through [client]. Resolved via Injekt, registered in
     *  `FolioApp.onCreate`. */
    val network: NetworkHelper by injectLazy()

    open val client: OkHttpClient
        get() = network.client

    open val headers: Headers by lazy { headersBuilder().build() }

    protected open fun headersBuilder(): Headers.Builder = Headers.Builder()
        .add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36")

    override fun fetchPopularManga(page: Int): Observable<MangasPage> =
        client.newCall(popularMangaRequest(page)).asObservableSuccess().map { popularMangaParse(it) }

    abstract fun popularMangaRequest(page: Int): Request
    abstract fun popularMangaParse(response: Response): MangasPage

    override fun fetchSearchManga(page: Int, query: String, filters: FilterList): Observable<MangasPage> =
        client.newCall(searchMangaRequest(page, query, filters)).asObservableSuccess().map { searchMangaParse(it) }

    abstract fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request
    abstract fun searchMangaParse(response: Response): MangasPage

    override fun fetchLatestUpdates(page: Int): Observable<MangasPage> =
        client.newCall(latestUpdatesRequest(page)).asObservableSuccess().map { latestUpdatesParse(it) }

    open fun latestUpdatesRequest(page: Int): Request = throw UnsupportedOperationException("Not implemented")
    open fun latestUpdatesParse(response: Response): MangasPage = throw UnsupportedOperationException("Not implemented")

    override fun fetchMangaDetails(manga: SManga): Observable<SManga> =
        client.newCall(mangaDetailsRequest(manga)).asObservableSuccess()
            .map { mangaDetailsParse(it).apply { initialized = true } }

    open fun mangaDetailsRequest(manga: SManga): Request = GET(baseUrl + manga.url, headers)
    abstract fun mangaDetailsParse(response: Response): SManga

    override fun fetchChapterList(manga: SManga): Observable<List<SChapter>> =
        client.newCall(chapterListRequest(manga)).asObservableSuccess().map { chapterListParse(it) }

    open fun chapterListRequest(manga: SManga): Request = GET(baseUrl + manga.url, headers)
    abstract fun chapterListParse(response: Response): List<SChapter>

    override fun fetchPageList(chapter: SChapter): Observable<List<Page>> =
        client.newCall(pageListRequest(chapter)).asObservableSuccess().map { pageListParse(it) }

    open fun pageListRequest(chapter: SChapter): Request = GET(baseUrl + chapter.url, headers)
    abstract fun pageListParse(response: Response): List<Page>

    open fun imageUrlRequest(page: Page): Request = GET(page.imageUrl!!, headers)
    open fun imageUrlParse(response: Response): String = throw UnsupportedOperationException("Not implemented")

    open fun fetchImageUrl(page: Page): Observable<String> =
        client.newCall(imageUrlRequest(page)).asObservableSuccess().map { imageUrlParse(it) }

    open fun fetchImage(page: Page): Observable<Response> =
        client.newCall(imageRequest(page)).asObservableSuccess()

    open fun imageRequest(page: Page): Request = GET(page.imageUrl!!, headers)

    override fun getFilterList(): FilterList = FilterList()

    open fun getMangaUrl(manga: SManga): String = baseUrl + manga.url
    open fun getChapterUrl(chapter: SChapter): String = baseUrl + chapter.url

    override fun toString() = "$name (${lang.uppercase()})"
}
