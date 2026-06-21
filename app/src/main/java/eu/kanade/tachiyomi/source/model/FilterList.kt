package eu.kanade.tachiyomi.source.model

class FilterList(list: List<Filter<*>> = emptyList()) : ArrayList<Filter<*>>(list) {
    constructor(vararg filters: Filter<*>) : this(filters.asList())
}
