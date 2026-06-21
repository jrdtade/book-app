package com.folio.reader.ui.screens

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.folio.reader.data.RemoteBookInfo
import com.folio.reader.ui.LibraryViewModel
import com.folio.reader.ui.folioViewModel

/** Lets the user browse Google Images for an alternate cover and tap one to use it —
 *  the same workflow Moon Reader offers for picking custom book covers. */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CoverPickerScreen(bookId: String, back: () -> Unit) {
    val vm: LibraryViewModel = folioViewModel()
    val books by vm.books.collectAsState()
    val book = books.firstOrNull { it.id == bookId } ?: run {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Book not found") }
        return
    }
    var loading by remember { mutableStateOf(true) }
    var picking by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    addJavascriptInterface(
                        object {
                            @JavascriptInterface
                            fun onImagePicked(url: String) {
                                picking = true
                                vm.setCoverFromUrl(book, url)
                                back()
                            }
                        },
                        "CoverBridge",
                    )
                    webViewClient = object : android.webkit.WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            loading = false
                            // Tapping a result thumbnail reports its image URL back to native code.
                            view?.evaluateJavascript(
                                """
                                (function() {
                                  document.querySelectorAll('img').forEach(function(img) {
                                    img.addEventListener('click', function(e) {
                                      e.preventDefault();
                                      var src = img.src && img.src.indexOf('http') === 0 ? img.src : img.getAttribute('data-src');
                                      if (src && window.CoverBridge) window.CoverBridge.onImagePicked(src);
                                    }, true);
                                  });
                                })();
                                """.trimIndent(),
                                null,
                            )
                        }
                    }
                    loadUrl(RemoteBookInfo.coverSearchUrl(book.title, book.author))
                }
            },
        )

        Row(
            Modifier.fillMaxWidth().padding(8.dp, 36.dp, 8.dp, 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = back) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
        }

        if (loading || picking) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}
