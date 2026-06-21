package eu.kanade.tachiyomi.source.model

import java.net.URI
import java.net.URISyntaxException

/** Strips the scheme/host from a full URL, keeping path/query/fragment, mirroring
 *  what real Tachiyomi/Mihon sources store as their stable `url` key. */
internal fun urlWithoutDomain(orig: String): String = try {
    val uri = URI(orig)
    buildString {
        append(uri.path)
        uri.query?.let { append('?').append(it) }
        uri.fragment?.let { append('#').append(it) }
    }
} catch (e: URISyntaxException) {
    orig
}
