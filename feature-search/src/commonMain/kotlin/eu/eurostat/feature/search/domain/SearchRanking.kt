package eu.eurostat.feature.search.domain

/**
 * Rank [entries] against a free-text [query], best match first.
 *
 * Matching is case-insensitive and purely lexical (no fuzzy matching). Every
 * entry is assigned to the best tier it satisfies, and tiers order the output:
 *
 *  1. the label starts with the query          ("gdp" → "GDP …")
 *  2. a word inside the label starts with it   ("infl" → "HICP inflation")
 *  3. the label contains it as a substring     ("icp" → "HICP inflation")
 *  4. a keyword contains it                    ("co2" → emission indicators)
 *  5. the description contains it              ("median" → poverty rate)
 *
 * Within a tier the original index order is preserved (the sort is stable),
 * so results stay in canonical module order among equals.
 *
 * A blank query yields an empty list — the browse state is served separately
 * by [browseSections].
 */
fun rankIndicators(
    query: String,
    entries: List<IndicatorEntry> = SearchIndex.entries,
): List<IndicatorEntry> {
    val normalized = query.trim().lowercase()
    if (normalized.isEmpty()) return emptyList()
    return entries
        .mapNotNull { entry -> matchTier(entry, normalized)?.let { tier -> tier to entry } }
        .sortedBy { (tier, _) -> tier }
        .map { (_, entry) -> entry }
}

/**
 * Group [entries] by module in [SearchModule] declaration order, for the
 * empty-query browse list. Modules without entries are omitted.
 */
fun browseSections(
    entries: List<IndicatorEntry> = SearchIndex.entries,
): List<SearchSection> =
    SearchModule.entries.mapNotNull { module ->
        val moduleEntries = entries.filter { it.module == module }
        if (moduleEntries.isEmpty()) null else SearchSection(module, moduleEntries)
    }

/** Word boundaries inside labels: anything that is not a letter, digit or '&'. */
private val WORD_SEPARATORS = Regex("[^\\p{L}\\p{N}&]+")

/**
 * Best (lowest) match tier for [entry] against the already-normalized
 * [query], or null when the entry does not match at all.
 */
private fun matchTier(entry: IndicatorEntry, query: String): Int? {
    val label = entry.label.lowercase()
    return when {
        label.startsWith(query) -> 0
        label.split(WORD_SEPARATORS).any { it.startsWith(query) } -> 1
        label.contains(query) -> 2
        entry.keywords.any { it.lowercase().contains(query) } -> 3
        entry.description.lowercase().contains(query) -> 4
        else -> null
    }
}
