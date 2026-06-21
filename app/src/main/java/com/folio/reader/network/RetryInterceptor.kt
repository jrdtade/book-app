package com.folio.reader.network

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/** Retries a request a bounded number of times on transient I/O failures (timeouts,
 *  dropped connections), which matters once requests start hitting real network sources
 *  instead of just local files. Does not retry on HTTP error status codes - those are a
 *  source's problem to interpret, not something to silently paper over here. */
class RetryInterceptor(private val maxRetries: Int = 2) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var lastException: IOException? = null
        repeat(maxRetries + 1) { attempt ->
            try {
                return chain.proceed(chain.request())
            } catch (e: IOException) {
                lastException = e
                if (attempt == maxRetries) throw e
            }
        }
        throw lastException ?: IOException("Request failed with no response")
    }
}
