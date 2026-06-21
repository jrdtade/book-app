package com.folio.reader.network.auth

import android.annotation.SuppressLint
import android.os.Bundle
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.folio.reader.network.NetworkConstants
import java.util.concurrent.CountDownLatch

/**
 * A dedicated Activity to handle Cloudflare-style challenges or manual logins.
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

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val url = intent.getStringExtra(EXTRA_URL) ?: return finish()

        setContent {
            Scaffold(
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = { finish() },
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
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.userAgentString = NetworkConstants.USER_AGENT
                                
                                webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        // If the user reaches a successful state, we could auto-finish
                                        // But often manual confirmation via FAB is safer for CF challenges.
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
        // Signal the interceptor that we are done
        currentLatch?.countDown()
        currentLatch = null
    }
}
