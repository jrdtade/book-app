package eu.kanade.tachiyomi.source

/** Entry point for multi-source extensions (e.g. one apk covering several languages
 *  of the same site). Named identically to our own [com.folio.reader.extension.SourceFactory]
 *  but in a different package — they're unrelated; this one is the real Tachiyomi contract. */
interface SourceFactory {
    fun createSources(): List<Source>
}
