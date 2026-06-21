package eu.kanade.tachiyomi.source.model

/**
 * Real working counterpart to the throwing stub published in Keiyoushi's
 * `extensions-lib` (which extensions compile against with `compileOnly`, so none
 * of its bytecode ships inside the extension apk). Since this app's classloader
 * is the parent of every extension's [dalvik.system.DexClassLoader], it's *this*
 * class — not the stub — that an extension's `SManga.create()` call resolves to
 * at runtime. The interface shape must match the published stub exactly.
 */
@Suppress("unused")
interface SManga {
    var url: String
    var title: String
    var artist: String?
    var author: String?
    var description: String?
    var genre: String?
    var status: Int
    var thumbnail_url: String?
    var update_strategy: UpdateStrategy
    var initialized: Boolean

    companion object {
        const val UNKNOWN = 0
        const val ONGOING = 1
        const val COMPLETED = 2
        const val LICENSED = 3
        const val PUBLISHING_FINISHED = 4
        const val CANCELLED = 5
        const val ON_HIATUS = 6

        fun create(): SManga = SMangaImpl()
    }
}

fun SManga.copyFrom(other: SManga) {
    if (other.author != null) author = other.author
    if (other.artist != null) artist = other.artist
    if (other.description != null) description = other.description
    if (other.genre != null) genre = other.genre
    if (other.thumbnail_url != null) thumbnail_url = other.thumbnail_url
    status = other.status
    if (!initialized) initialized = other.initialized
}

fun SManga.setUrlWithoutDomain(url: String) {
    this.url = urlWithoutDomain(url)
}

private class SMangaImpl : SManga {
    override var url: String = ""
    override var title: String = ""
    override var artist: String? = null
    override var author: String? = null
    override var description: String? = null
    override var genre: String? = null
    override var status: Int = SManga.UNKNOWN
    override var thumbnail_url: String? = null
    override var update_strategy: UpdateStrategy = UpdateStrategy.ALWAYS_UPDATE
    override var initialized: Boolean = false
}
