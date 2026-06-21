package com.folio.reader.extension

import com.folio.reader.source.MediaSource

/**
 * The contract an extension's code file must implement. [ExtensionManager] locates
 * the class named in the extension's manifest (`factoryClass`) via reflection and
 * calls [createSources] to obtain the [MediaSource]s it contributes. Mirrors how
 * Mihon extensions expose a `Source`/`SourceFactory` entry point, but scoped down to
 * a single factory class per extension instead of a package-scanning convention.
 */
interface SourceFactory {
    fun createSources(): List<MediaSource>
}
