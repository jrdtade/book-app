package com.folio.reader.network.cookies

import okhttp3.Cookie
import okhttp3.HttpUrl

/** Persists cookies for an authenticated source's session across app restarts. */
interface CookieStore {
    suspend fun saveCookies(url: HttpUrl, cookies: List<Cookie>)
    suspend fun getCookies(url: HttpUrl): List<Cookie>
    suspend fun clearCookies(url: HttpUrl)
}
