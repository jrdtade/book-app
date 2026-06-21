package com.folio.reader.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.UUID

/** Talks to the public Google Books / Google Images web endpoints to fill in
 *  details the EPUB itself doesn't carry: a synopsis, and alternate cover art. */
object RemoteBookInfo {

    suspend fun fetchSynopsis(title: String, author: String): String? = withContext(Dispatchers.IO) {
        val query = URLEncoder.encode("intitle:$title intitle:${author.ifBlank { title }}", "UTF-8")
        val url = URL("https://www.googleapis.com/books/v1/volumes?q=$query&maxResults=1")
        val body = url.openConnection().let { conn ->
            conn as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.inputStream.bufferedReader().use { it.readText() }
        }
        val items = JSONObject(body).optJSONArray("items") ?: return@withContext null
        if (items.length() == 0) return@withContext null
        items.getJSONObject(0).optJSONObject("volumeInfo")?.optString("description")?.takeIf { it.isNotBlank() }
    }

    /** Searches Google Images for "<title> <author> book cover" and returns the result page HTML
     *  so a WebView can render it and let the user tap an image to use as the new cover. */
    fun coverSearchUrl(title: String, author: String): String {
        val query = URLEncoder.encode("$title $author book cover", "UTF-8")
        return "https://www.google.com/search?tbm=isch&q=$query"
    }

    /** Downloads the picked image into the book's own content directory and returns
     *  the path to store as [Book.coverPath]. */
    suspend fun downloadCover(contentDir: String, imageUrl: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            val ext = if (imageUrl.contains(".png", ignoreCase = true)) "png" else "jpg"
            val dest = File(contentDir, "cover_custom_${UUID.randomUUID()}.$ext")
            val conn = URL(imageUrl).openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 15_000
            conn.instanceFollowRedirects = true
            conn.inputStream.use { input -> dest.outputStream().use { output -> input.copyTo(output) } }
            dest.absolutePath
        }.getOrNull()
    }
}
