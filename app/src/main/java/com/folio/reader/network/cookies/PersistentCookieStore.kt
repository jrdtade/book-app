package com.folio.reader.network.cookies

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import okhttp3.Cookie
import okhttp3.HttpUrl

private val Context.cookieDataStore by preferencesDataStore(name = "folio_cookies")

/** DataStore-backed [CookieStore], keyed by the cookie's domain so cookies for one
 *  source's host don't leak into requests to another. */
class PersistentCookieStore(private val context: Context) : CookieStore {

    override suspend fun saveCookies(url: HttpUrl, cookies: List<Cookie>) {
        val key = stringPreferencesKey(url.host)
        context.cookieDataStore.edit { prefs ->
            val existing = prefs[key]?.let { deserialize(it) } ?: emptyMap()
            val merged = existing.toMutableMap()
            cookies.forEach { merged[it.name] = it }
            prefs[key] = serialize(merged.values)
        }
    }

    override suspend fun getCookies(url: HttpUrl): List<Cookie> {
        val key = stringPreferencesKey(url.host)
        val stored = context.cookieDataStore.data.first()[key] ?: return emptyList()
        val now = System.currentTimeMillis()
        return deserialize(stored).values
            .filter { it.expiresAt > now }
            .filter { it.matches(url) }
    }

    override suspend fun clearCookies(url: HttpUrl) {
        val key = stringPreferencesKey(url.host)
        context.cookieDataStore.edit { prefs -> prefs.remove(key) }
    }

    private fun serialize(cookies: Collection<Cookie>): String =
        cookies.joinToString(ENTRY_DELIMITER) { cookie ->
            listOf(
                cookie.name,
                cookie.value,
                cookie.domain,
                cookie.path,
                cookie.expiresAt.toString(),
                cookie.secure.toString(),
                cookie.httpOnly.toString(),
                cookie.hostOnly.toString(),
            ).joinToString(FIELD_DELIMITER)
        }

    private fun deserialize(raw: String): Map<String, Cookie> {
        if (raw.isBlank()) return emptyMap()
        return raw.split(ENTRY_DELIMITER)
            .mapNotNull { entry ->
                val fields = entry.split(FIELD_DELIMITER)
                if (fields.size != 8) return@mapNotNull null
                val name = fields[0]
                val value = fields[1]
                val domain = fields[2]
                val path = fields[3]
                val expiresAt = fields[4].toLongOrNull() ?: Long.MAX_VALUE
                val secure = fields[5].toBoolean()
                val httpOnly = fields[6].toBoolean()
                val hostOnly = fields[7].toBoolean()
                val builder = Cookie.Builder()
                    .name(name)
                    .value(value)
                    .path(path)
                    .expiresAt(expiresAt)
                if (hostOnly) builder.hostOnlyDomain(domain) else builder.domain(domain)
                if (secure) builder.secure()
                if (httpOnly) builder.httpOnly()
                name to builder.build()
            }
            .toMap()
    }

    private companion object {
        // Control characters are disallowed in cookie names/values (RFC 6265), so they're
        // safe to use as serialization delimiters here.
        const val ENTRY_DELIMITER = ""
        const val FIELD_DELIMITER = ""
    }
}
