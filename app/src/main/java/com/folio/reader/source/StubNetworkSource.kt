package com.folio.reader.source

import com.folio.reader.data.MediaType

/**
 * Placeholder for a remote extension/source. Establishes the [MediaSource] fetching
 * contract that a real network-backed source would implement once there's a specific,
 * authorized endpoint to target (an API with a key, a legal content partner, etc.) -
 * this stub isn't wired to any live endpoint.
 */
class StubNetworkSource(
    override val id: String,
    override val name: String,
    override val mediaType: MediaType,
) : MediaSource {
    override suspend fun fetchLatestUpdates(): List<SourceMediaInfo> =
        throw NotImplementedError("$name has no backing endpoint configured yet")

    override suspend fun fetchMediaDetails(sourceMediaId: String): SourceMediaDetails =
        throw NotImplementedError("$name has no backing endpoint configured yet")

    override suspend fun fetchChapterList(sourceMediaId: String): List<SourceChapter> =
        throw NotImplementedError("$name has no backing endpoint configured yet")

    override suspend fun fetchPageList(chapterId: String): List<SourcePage> =
        throw NotImplementedError("$name has no backing endpoint configured yet")
}
