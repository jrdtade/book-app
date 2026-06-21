package com.folio.reader.network

import okhttp3.Interceptor
import okhttp3.Response

/** Pins every outgoing request to [NetworkConstants.USER_AGENT] so a source's session
 *  (and any cookies tied to it) stays consistent with the identity a login WebView used. */
class UserAgentInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header("User-Agent", NetworkConstants.USER_AGENT)
            .build()
        return chain.proceed(request)
    }
}
