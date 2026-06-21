package com.folio.reader.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** A cover candidate surfaced for the "choose an alternate cover" picker. */
data class CoverCandidate(
    val volumeId: String,
    val title: String,
    val author: String,
    val thumbnailUrl: String,
)

/** Client for the Open Library APIs (search.json / works / covers). Unlike the Google
 *  Books API, these endpoints are meant for unauthenticated public use and don't sit
 *  behind a daily quota, which is what was silently breaking synopsis lookups and the
 *  cover picker before (Google Books now rejects most key-less traffic with a 429). */
object OpenLibraryApi {
    private const val SEARCH_URL = "https://openlibrary.org/search.json"
    private const val WORKS_URL = "https://openlibrary.org"
    private const val COVERS_URL = "https://covers.openlibrary.org/b/id"

    suspend fun fetchMetadata(title: String, author: String): Pair<String?, String?> = withContext(Dispatchers.IO) {
        val doc = search(title, author, limit = 1) ?: return@withContext null to null
        val docs = doc.optJSONArray("docs")
        if (docs == null || docs.length() == 0) return@withContext null to null
        val first = docs.getJSONObject(0)
        val publishedDate = first.optInt("first_publish_year", -1).takeIf { it > 0 }?.toString()
        val workKey = first.optString("key", "").takeIf { it.isNotBlank() }
        val synopsis = workKey?.let { fetchWorkDescription(it) }
        synopsis to publishedDate
    }

    suspend fun searchCovers(query: String): List<CoverCandidate> = withContext(Dispatchers.IO) {
        val doc = search(query, "", limit = 20) ?: return@withContext emptyList()
        val docs = doc.optJSONArray("docs") ?: return@withContext emptyList()
        val results = mutableListOf<CoverCandidate>()
        for (i in 0 until docs.length()) {
            val d = docs.getJSONObject(i)
            val coverId = d.optInt("cover_i", -1)
            if (coverId <= 0) continue
            val key = d.optString("key", "")
            if (key.isBlank()) continue
            results.add(
                CoverCandidate(
                    volumeId = key,
                    title = d.optString("title", ""),
                    author = d.optJSONArray("author_name")?.optString(0, "") ?: "",
                    thumbnailUrl = "$COVERS_URL/$coverId-L.jpg",
                ),
            )
        }
        results
    }

    private fun fetchWorkDescription(workKey: String): String? {
        val json = httpGet("$WORKS_URL$workKey.json") ?: return null
        val desc = json.opt("description") ?: return null
        return when (desc) {
            is String -> desc
            is JSONObject -> desc.optString("value", "").takeIf { it.isNotBlank() }
            else -> null
        }
    }

    private fun search(title: String, author: String, limit: Int): JSONObject? {
        val q = buildString {
            append(URLEncoder.encode(title, "UTF-8"))
            if (author.isNotBlank() && author != "Unknown") {
                append("+").append(URLEncoder.encode(author, "UTF-8"))
            }
        }
        return httpGet("$SEARCH_URL?q=$q&limit=$limit&fields=key,title,author_name,first_publish_year,cover_i")
    }

    private fun httpGet(url: String): JSONObject? {
        val connection = URL(url).openConnection() as HttpURLConnection
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
