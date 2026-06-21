package com.folio.reader.network.cookies

import android.webkit.CookieManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import okhttp3.Cookie
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl

/** Notifies listeners (e.g. a paused request waiting on a login) once a host's session
 *  cookies have been synced from a WebView into [CookieStore]. */
object CookieSyncBus {
    private val _updates = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val updates: SharedFlow<String> = _updates

    suspend fun notifySynced(host: String) {
        _updates.emit(host)
    }
}

/** Reads the cookies [android.webkit.CookieManager] has accumulated for [url] (e.g. after
 *  the user finished logging into a source inside an in-app WebView), persists them via
 *  [store] so OkHttp's [AndroidCookieJar] picks them up on the next request to that host,
 *  and notifies [CookieSyncBus] so any caller waiting on the session can resume. */
suspend fun syncCookiesFromWebView(url: String, store: CookieStore) {
    val httpUrl = url.toHttpUrl()
    val rawCookieHeader = CookieManager.getInstance().getCookie(url) ?: return
    val cookies = parseCookieHeader(rawCookieHeader, httpUrl)
    if (cookies.isNotEmpty()) {
        store.saveCookies(httpUrl, cookies)
        CookieSyncBus.notifySynced(httpUrl.host)
    }
}

/** [CookieManager.getCookie] returns a single "name=value; name2=value2" header rather
 *  than the Set-Cookie form [Cookie.parse] expects, so each pair is parsed individually. */
private fun parseCookieHeader(rawCookieHeader: String, url: HttpUrl): List<Cookie> =
    rawCookieHeader.split(";")
        .mapNotNull { pair -> Cookie.parse(url, pair.trim()) }
