package eu.kanade.tachiyomi.source

import androidx.preference.PreferenceScreen

/** Present so login/settings-capable extensions can classload. This app has no
 *  extension settings UI yet, so [setupPreferenceScreen] is never actually called —
 *  such extensions will install and fetch content, but can't be configured. */
interface ConfigurableSource : Source {
    fun setupPreferenceScreen(screen: PreferenceScreen)
}
