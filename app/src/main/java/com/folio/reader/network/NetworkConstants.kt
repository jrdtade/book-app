package com.folio.reader.network

/** Shared networking constants so every HTTP client and WebView in the app present
 *  the same identity, which matters for sources that key sessions to a User-Agent. */
object NetworkConstants {
    const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/126.0.0.0 Mobile Safari/537.36"
}
