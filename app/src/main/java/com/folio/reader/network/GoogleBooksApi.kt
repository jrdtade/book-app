package com.folio.reader.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** A book cover candidate returned by a Google Books search, used for both the
 *  synopsis lookup and the "choose an alternate cover" picker. */
data class CoverCandidate(
    val volumeId: String,
    val title: String,
    val author: String,
    val thumbnailUrl: String,
)

/** Thin client for the public (key-less) Google Books volumes API. Used to fetch a
 *  synopsis for a book, and to surface cover art alternatives the same way an image
 *  search for "<title> <author> book cover" would. */
object GoogleBooksApi {
    private const val BASE_URL = "https://www.googleapis.com/books/v1/volumes"

    suspend fun fetchSynopsis(title: String, author: String): String? = withContext(Dispatchers.IO) {
        val items = search(title, author, maxResults = 1)
        items.firstOrNull()
    }

    suspend fun searchCovers(query: String): List<CoverCandidate> = withContext(Dispatchers.IO) {
        searchVolumes(query, maxResults = 20)
    }

    private fun search(title: String, author: String, maxResults: Int): List<String?> {
        val query = "intitle:$title" + if (author.isNotBlank() && author != "Unknown") "+inauthor:$author" else ""
        val json = httpGet(query, maxResults) ?: return emptyList()
        val items = json.optJSONArray("items") ?: return emptyList()
        val results = mutableListOf<String?>()
        for (i in 0 until items.length()) {
            val info = items.getJSONObject(i).optJSONObject("volumeInfo") ?: continue
            val description = info.optString("description", "").takeIf { it.isNotBlank() }
            if (description != null) results.add(description)
        }
        return results
    }

    private fun searchVolumes(query: String, maxResults: Int): List<CoverCandidate> {
        val json = httpGet(query, maxResults) ?: return emptyList()
        val items = json.optJSONArray("items") ?: return emptyList()
        val results = mutableListOf<CoverCandidate>()
        for (i in 0 until items.length()) {
            val item = items.getJSONObject(i)
            val volumeId = item.optString("id", "")
            if (volumeId.isBlank()) continue
            val info = item.optJSONObject("volumeInfo") ?: continue
            val images = info.optJSONObject("imageLinks") ?: continue
            val rawThumb = images.optString("thumbnail", "").ifBlank { images.optString("smallThumbnail", "") }
            if (rawThumb.isBlank()) continue
            results.add(
                CoverCandidate(
                    volumeId = volumeId,
                    title = info.optString("title", ""),
                    author = info.optJSONArray("authors")?.optString(0, "") ?: "",
                    // Google Books thumbnails are served small by default (zoom=1); a higher zoom
                    // level returns sharper art, which is what we want for a cover image.
                    thumbnailUrl = rawThumb.replace("http://", "https://").replace("zoom=1", "zoom=3"),
                ),
            )
        }
        return results
    }

    private fun httpGet(query: String, maxResults: Int): JSONObject? {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = URL("$BASE_URL?q=$encoded&maxResults=$maxResults")
        val connection = url.openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.requestMethod = "GET"
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            JSONObject(body)
        } catch (_: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }
}
