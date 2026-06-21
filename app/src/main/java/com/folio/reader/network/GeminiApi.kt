package com.folio.reader.network

import com.folio.reader.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Genre, descriptive tags, synopsis and publication info gathered for a book by Gemini. */
data class BookClassification(
    val genre: String,
    val tags: List<String>,
    val synopsis: String?,
    val publishedDate: String?,
)

/** Client for the Gemini API's generateContent endpoint, used to classify a book's
 *  genre/tags and look up its synopsis and publication date from its title/author.
 *  Asks for a strict JSON response so no free-text parsing is needed. */
object GeminiApi {
    private const val MODEL = "gemini-2.0-flash"
    private const val ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent"

    suspend fun classify(title: String, author: String): BookClassification? = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank()) return@withContext null

        val prompt = "Book title: \"$title\" by $author. " +
            "Reply with ONLY a JSON object of the form " +
            "{\"genre\": \"<a single genre label>\", \"tags\": [\"<tag1>\", \"<tag2>\", \"<tag3>\"], " +
            "\"synopsis\": \"<a 2-4 sentence synopsis of the book>\", " +
            "\"publishedDate\": \"<original publication year or date, or null if unknown>\"}. " +
            "No markdown, no extra text."

        val requestBody = JSONObject().apply {
            put(
                "contents",
                JSONArray().put(
                    JSONObject().put(
                        "parts",
                        JSONArray().put(JSONObject().put("text", prompt)),
                    ),
                ),
            )
            put("generationConfig", JSONObject().put("responseMimeType", "application/json"))
        }

        val responseJson = httpPost("$ENDPOINT?key=$apiKey", requestBody) ?: return@withContext null
        val text = responseJson.optJSONArray("candidates")
            ?.optJSONObject(0)
            ?.optJSONObject("content")
            ?.optJSONArray("parts")
            ?.optJSONObject(0)
            ?.optString("text", "")
            ?.takeIf { it.isNotBlank() } ?: return@withContext null

        runCatching {
            val parsed = JSONObject(text)
            val genre = parsed.optString("genre", "").takeIf { it.isNotBlank() } ?: return@runCatching null
            val tagsArray = parsed.optJSONArray("tags") ?: JSONArray()
            val tags = (0 until tagsArray.length()).mapNotNull { i -> tagsArray.optString(i, "").takeIf { it.isNotBlank() } }
            val synopsis = parsed.optString("synopsis", "").takeIf { it.isNotBlank() }
            val publishedDate = parsed.optString("publishedDate", "").takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
            BookClassification(genre, tags, synopsis, publishedDate)
        }.getOrNull()
    }

    private fun httpPost(url: String, body: JSONObject): JSONObject? {
        val connection = URL(url).openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.outputStream.use { it.write(body.toString().toByteArray()) }
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
            val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
            JSONObject(responseBody)
        } catch (_: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }
}
