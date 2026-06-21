package com.folio.reader.network

import android.webkit.WebView

/** Applies the app's standard settings to a WebView used for an authenticated source
 *  login flow, so its User-Agent matches [NetworkConstants.USER_AGENT] and the session
 *  it establishes lines up with what [com.folio.reader.network.cookies.AndroidCookieJar]
 *  will later attach to OkHttp requests for that source. */
fun WebView.applyStandardSettings() {
    settings.userAgentString = NetworkConstants.USER_AGENT
    settings.javaScriptEnabled = true
    settings.domStorageEnabled = true
}
