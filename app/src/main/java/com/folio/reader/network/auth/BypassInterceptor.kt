package com.folio.reader.network.auth

import android.content.Context
import android.content.Intent
import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Intercepts 403/503 errors and Cloudflare challenges to launch [AuthWebViewActivity].
 */
class BypassInterceptor(private val context: Context) : Interceptor {
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val response = chain.proceed(originalRequest)

        // Detect Cloudflare/DDoS protection even if it returns 200 OK
        val isChallenge = response.code == 403 || response.code == 503 || isChallengeBody(response)

        if (isChallenge) {
            response.close()

            val latch = CountDownLatch(1)
            AuthWebViewActivity.setLatch(latch)

            val intent = Intent(context, AuthWebViewActivity::class.java).apply {
                putExtra(AuthWebViewActivity.EXTRA_URL, originalRequest.url.toString())
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)

            // Block the OkHttp thread until the activity finishes (up to 2 minutes)
            try {
                latch.await(2, TimeUnit.MINUTES)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }

            // Retry the request with new cookies
            return chain.proceed(originalRequest.newBuilder().build())
        }

        return response
    }

    private fun isChallengeBody(response: Response): Boolean {
        val body = response.peekBody(1024 * 64).string().lowercase()
        return body.contains("checking your browser") || 
               body.contains("cf-browser-verification") ||
               body.contains("captcha") ||
               body.contains("ray id:")
    }
}
