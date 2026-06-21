package com.folio.reader.network.auth

import android.content.Context
import android.content.Intent
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Detects a Cloudflare challenge and resolves it by opening [AuthWebViewActivity] so the
 * user (or the JS challenge itself) can pass it, syncing the resulting cookies back into
 * the shared cookie jar, then retrying the request once.
 *
 * Trigger logic mirrors Mihon's CloudflareInterceptor: ONLY a 403/503 carrying a Cloudflare
 * `Server` header. The previous version sniffed the body for the bare word "captcha"/"ray id:"
 * which false-positived on ordinary pages (e.g. BatCave's listing), launching the WebView on
 * every load.
 *
 * Critically, an OkHttp interceptor must only ever throw [IOException]: OkHttp routes an
 * IOException to the call's onFailure, but RE-THROWS any other Throwable on its dispatcher
 * thread, which is uncaught and crashes the whole process. So everything here is wrapped to
 * surface only IOException.
 */
class BypassInterceptor(private val context: Context) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())

        if (!response.isCloudflareChallenge()) {
            return response
        }

        response.close()

        try {
            resolveWithWebView(chain.request())
        } catch (e: IOException) {
            throw e
        } catch (e: Exception) {
            // startActivity (background-launch restrictions), latch handling, etc. can throw
            // non-IOException — convert so OkHttp doesn't re-throw it on its thread and crash.
            throw IOException("Cloudflare bypass failed: ${e.message}", e)
        }

        // Retry once with whatever cookies the WebView established.
        return chain.proceed(chain.request().newBuilder().build())
    }

    private fun Response.isCloudflareChallenge(): Boolean {
        if (code !in CHALLENGE_CODES) return false
        val server = header("Server").orEmpty()
        return CLOUDFLARE_SERVERS.any { server.startsWith(it, ignoreCase = true) }
    }

    private fun resolveWithWebView(request: Request) {
        val latch = CountDownLatch(1)
        AuthWebViewActivity.setLatch(latch)

        val intent = Intent(context, AuthWebViewActivity::class.java).apply {
            putExtra(AuthWebViewActivity.EXTRA_URL, request.url.toString())
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)

        // Block the OkHttp thread until the activity finishes (up to 2 minutes).
        latch.await(2, TimeUnit.MINUTES)
    }

    private companion object {
        val CHALLENGE_CODES = listOf(403, 503)
        val CLOUDFLARE_SERVERS = listOf("cloudflare", "cloudflare-nginx")
    }
}
