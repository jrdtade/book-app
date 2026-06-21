package com.folio.reader.source

/**
 * Our own simplified mirror of Tachiyomi's `Filter` hierarchy, so non-extension
 * sources (local files, stub network source) aren't forced to depend on
 * `eu.kanade.tachiyomi.*` types. [com.folio.reader.extension.TachiyomiSourceAdapter]
 * translates real `Filter<*>` instances to/from this shape.
 */
sealed class SourceFilter(val name: String) {
    class Header(name: String) : SourceFilter(name)
    class Separator(name: String = "") : SourceFilter(name)
    class Text(name: String, var value: String = "") : SourceFilter(name)
    class CheckBox(name: String, var checked: Boolean = false) : SourceFilter(name)

    /** 0 = ignored, 1 = included, 2 = excluded — matches Tachiyomi's TriState. */
    class TriState(name: String, var state: Int = 0) : SourceFilter(name)

    class Select(name: String, val options: List<String>, var selected: Int = 0) : SourceFilter(name)
    class MultiSelect(name: String, val options: List<String>, var selected: Set<Int> = emptySet()) : SourceFilter(name)
    class Group(name: String, val filters: List<SourceFilter>) : SourceFilter(name)
    class Sort(
        name: String,
        val options: List<String>,
        var selectedIndex: Int? = null,
        var ascending: Boolean = true,
    ) : SourceFilter(name)
}
