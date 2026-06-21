package eu.kanade.tachiyomi.network

import okhttp3.CacheControl
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.Request
import okhttp3.RequestBody

fun GET(url: String, headers: Headers = Headers.Builder().build(), cache: CacheControl? = null): Request {
    val builder = Request.Builder().url(url).headers(headers)
    if (cache != null) builder.cacheControl(cache)
    return builder.build()
}

fun GET(url: HttpUrl, headers: Headers = Headers.Builder().build(), cache: CacheControl? = null): Request {
    val builder = Request.Builder().url(url).headers(headers)
    if (cache != null) builder.cacheControl(cache)
    return builder.build()
}

fun POST(
    url: String,
    headers: Headers = Headers.Builder().build(),
    body: RequestBody = FormBody.Builder().build(),
    cache: CacheControl? = null,
): Request {
    val builder = Request.Builder().url(url).post(body).headers(headers)
    if (cache != null) builder.cacheControl(cache)
    return builder.build()
}
