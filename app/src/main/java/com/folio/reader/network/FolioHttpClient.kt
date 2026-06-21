package com.folio.reader.network

import android.content.Context
import com.folio.reader.network.auth.BypassInterceptor
import com.folio.reader.network.cookies.AndroidCookieJar
import com.folio.reader.network.cookies.PersistentCookieStore
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/** The app's single OkHttpClient, shared by every network-backed [com.folio.reader.source.MediaSource].
 *  Carries a persistent cookie jar so a session started in a login WebView (see
 *  [com.folio.reader.network.cookies.syncCookiesFromWebView]) survives across requests
 *  and app restarts, plus a consistent User-Agent and bounded retries. Per-source auth
 *  headers can be added by passing providers to [authProvidersByHost]. */
object FolioHttpClient {
    @Volatile private var instance: OkHttpClient? = null

    fun get(context: Context, authProvidersByHost: Map<String, AuthHeaderProvider> = emptyMap()): OkHttpClient =
        instance ?: synchronized(this) {
            instance ?: build(context, authProvidersByHost).also { instance = it }
        }

    private fun build(context: Context, authProvidersByHost: Map<String, AuthHeaderProvider>): OkHttpClient =
        OkHttpClient.Builder()
            .cookieJar(AndroidCookieJar(PersistentCookieStore(context.applicationContext)))
            .addInterceptor(UserAgentInterceptor())
            .addInterceptor(BypassInterceptor(context))
            .addInterceptor(AuthInterceptor(authProvidersByHost))
            .addInterceptor(RetryInterceptor())
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
}
