package com.folio.reader.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.readerDataStore by preferencesDataStore(name = "folio_prefs")

enum class PageFlip { CURL, SLIDE, FADE, NONE }

data class ReaderPrefs(
    val theme: String = "sepia",
    val font: String = "serif",
    val size: Int = 20,
    val lineHeight: Float = 1.62f,
    val margin: Int = 30,
    val bold: Boolean = false,
    val justify: Boolean = false,
    val brightness: Float = 1f,
    val warmth: Float = 0f,
    val flip: PageFlip = PageFlip.CURL,
    val scrollMode: Boolean = false,
)

object PrefKeys {
    val THEME = stringPreferencesKey("theme")
    val FONT = stringPreferencesKey("font")
    val SIZE = intPreferencesKey("size")
    val LH = floatPreferencesKey("lh")
    val MARGIN = intPreferencesKey("margin")
    val BOLD = booleanPreferencesKey("bold")
    val JUSTIFY = booleanPreferencesKey("justify")
    val BRIGHTNESS = floatPreferencesKey("brightness")
    val WARMTH = floatPreferencesKey("warmth")
    val FLIP = stringPreferencesKey("flip")
    val SCROLL = booleanPreferencesKey("scroll")
}

class ReaderPrefsRepository(private val context: Context) {
    val prefsFlow: Flow<ReaderPrefs> = context.readerDataStore.data.map { p ->
        ReaderPrefs(
            theme = p[PrefKeys.THEME] ?: "sepia",
            font = p[PrefKeys.FONT] ?: "serif",
            size = p[PrefKeys.SIZE] ?: 20,
            lineHeight = p[PrefKeys.LH] ?: 1.62f,
            margin = p[PrefKeys.MARGIN] ?: 30,
            bold = p[PrefKeys.BOLD] ?: false,
            justify = p[PrefKeys.JUSTIFY] ?: false,
            brightness = p[PrefKeys.BRIGHTNESS] ?: 1f,
            warmth = p[PrefKeys.WARMTH] ?: 0f,
            flip = runCatching { PageFlip.valueOf(p[PrefKeys.FLIP] ?: "CURL") }.getOrDefault(PageFlip.CURL),
            scrollMode = p[PrefKeys.SCROLL] ?: false,
        )
    }

    suspend fun update(prefs: ReaderPrefs) {
        context.readerDataStore.edit { p ->
            p[PrefKeys.THEME] = prefs.theme
            p[PrefKeys.FONT] = prefs.font
            p[PrefKeys.SIZE] = prefs.size
            p[PrefKeys.LH] = prefs.lineHeight
            p[PrefKeys.MARGIN] = prefs.margin
            p[PrefKeys.BOLD] = prefs.bold
            p[PrefKeys.JUSTIFY] = prefs.justify
            p[PrefKeys.BRIGHTNESS] = prefs.brightness
            p[PrefKeys.WARMTH] = prefs.warmth
            p[PrefKeys.FLIP] = prefs.flip.name
            p[PrefKeys.SCROLL] = prefs.scrollMode
        }
    }
}
