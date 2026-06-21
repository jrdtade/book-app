package com.folio.reader.network.cookies

import kotlinx.coroutines.runBlocking
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

/** Bridges OkHttp's [CookieJar] contract to our persistent [CookieStore], so a session
 *  established once (e.g. by logging into a source) survives across requests and app
 *  restarts instead of living only in OkHttp's in-memory default jar. */
class AndroidCookieJar(private val store: CookieStore) : CookieJar {

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        if (cookies.isEmpty()) return
        runBlocking { store.saveCookies(url, cookies) }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> =
        runBlocking { store.getCookies(url) }
}
