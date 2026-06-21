package eu.kanade.tachiyomi.network

import android.content.Context
import okhttp3.OkHttpClient

/**
 * Real counterpart to the published stub, registered once with [uy.kohesive.injekt.Injekt]
 * in `FolioApp.onCreate` so any extension that does `Injekt.get<NetworkHelper>()`
 * gets a working instance. [cloudflareClient] is the same client as [client] — both
 * already run through [com.folio.reader.network.auth.BypassInterceptor], which on a
 * 403/503/challenge response launches [com.folio.reader.network.auth.AuthWebViewActivity]
 * to solve the JS challenge in a real WebView, syncs the resulting `cf_clearance`
 * cookie into the app's persistent cookie jar, and retries — a real bypass, not a stub.
 */
class NetworkHelper(context: Context, sharedClient: OkHttpClient) {
    val client: OkHttpClient = sharedClient
    val cloudflareClient: OkHttpClient = sharedClient
}
