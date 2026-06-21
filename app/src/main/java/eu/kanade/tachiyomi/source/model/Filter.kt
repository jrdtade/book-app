package eu.kanade.tachiyomi.source.model

/** Present so extension classes that declare custom filters in `getFilterList()`
 *  can classload; this app doesn't yet render a filter picker UI, so any filters
 *  a source returns are accepted but always passed back empty/default. */
@Suppress("unused")
abstract class Filter<T>(val name: String, var state: T) {

    open class Header(name: String) : Filter<Boolean>(name, false)
    open class Separator(name: String = "") : Filter<Boolean>(name, false)
    abstract class Text(name: String, state: String = "") : Filter<String>(name, state)
    abstract class CheckBox(name: String, state: Boolean = false) : Filter<Boolean>(name, state)

    abstract class TriState(name: String, state: Int = STATE_IGNORE) : Filter<Int>(name, state) {
        fun isIgnored() = state == STATE_IGNORE
        fun isExcluded() = state == STATE_EXCLUDE
        fun isIncluded() = state == STATE_INCLUDE

        companion object {
            const val STATE_IGNORE = 0
            const val STATE_INCLUDE = 1
            const val STATE_EXCLUDE = 2
        }
    }

    abstract class Select<V>(name: String, val values: Array<V>, state: Int = 0) : Filter<Int>(name, state)
    abstract class Group<V>(name: String, state: List<V>) : Filter<List<V>>(name, state)

    abstract class Sort(name: String, val values: Array<String>, state: Selection? = null) :
        Filter<Sort.Selection?>(name, state) {
        data class Selection(val index: Int, val ascending: Boolean)
    }
}
