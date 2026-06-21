package com.folio.reader.network

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

/** Supplies auth headers (API key, bearer token, etc.) for requests to a specific
 *  authorized source. Sources register a provider for their own host; hosts with no
 *  registered provider are left untouched. */
interface AuthHeaderProvider {
    suspend fun headersFor(host: String): Map<String, String>
}

/** Attaches per-source auth headers without every [com.folio.reader.source.MediaSource]
 *  needing to build its own OkHttpClient. */
class AuthInterceptor(private val providersByHost: Map<String, AuthHeaderProvider>) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val provider = providersByHost[request.url.host] ?: return chain.proceed(request)
        val headers = runBlocking { provider.headersFor(request.url.host) }
        if (headers.isEmpty()) return chain.proceed(request)
        val builder = request.newBuilder()
        headers.forEach { (name, value) -> builder.header(name, value) }
        return chain.proceed(builder.build())
    }
}
