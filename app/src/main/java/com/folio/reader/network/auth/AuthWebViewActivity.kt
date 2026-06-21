package com.folio.reader.network.auth

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.lifecycleScope
import com.folio.reader.network.applyStandardSettings
import com.folio.reader.network.cookies.PersistentCookieStore
import com.folio.reader.network.cookies.syncCookiesFromWebView
import kotlinx.coroutines.launch
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A dedicated Activity to handle Cloudflare-style challenges or manual logins.
 * Auto-detects a resolved Cloudflare challenge (presence of the `cf_clearance`
 * cookie) and finishes on its own; the FAB is a manual fallback for slower
 * challenges or sources that need an actual login rather than just a JS check.
 */
class AuthWebViewActivity : ComponentActivity() {

    companion object {
        const val EXTRA_URL = "extra_url"

        // Static latch to bridge between the OkHttp thread and this Activity
        @Volatile
        private var currentLatch: CountDownLatch? = null

        fun setLatch(latch: CountDownLatch) {
            currentLatch = latch
        }
    }

    private val finished = AtomicBoolean(false)

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val url = intent.getStringExtra(EXTRA_URL) ?: return finish()
        val cookieStore = PersistentCookieStore(applicationContext)

        // Without this, the WebView's cookies (e.g. Cloudflare's cf_clearance)
        // would only ever live in android.webkit.CookieManager — OkHttp's
        // AndroidCookieJar would never see them, and the retried request after
        // this activity closes would fail exactly like the first attempt did.
        fun finishAndSyncCookies() {
            if (!finished.compareAndSet(false, true)) return
            lifecycleScope.launch {
                syncCookiesFromWebView(url, cookieStore)
                finish()
            }
        }

        setContent {
            Scaffold(
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = { finishAndSyncCookies() },
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Done")
                    }
                }
            ) { padding ->
                Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                applyStandardSettings()

                                webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView?, finishedUrl: String?) {
                                        super.onPageFinished(view, finishedUrl)
                                        val cookies = CookieManager.getInstance().getCookie(finishedUrl ?: url)
                                        if (cookies?.contains("cf_clearance") == true) {
                                            finishAndSyncCookies()
                                        }
                                    }
                                }
                                loadUrl(url)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Signal the interceptor that we are done — by this point finishAndSyncCookies
        // has already persisted any cookies if the user (or auto-detection) triggered it.
        currentLatch?.countDown()
        currentLatch = null
    }
}
