package eu.eurostat.feature.search.domain

/**
 * A browse group: all indicators of one [module], in index order.
 * Produced by [browseSections] for the empty-query state of the Search screen.
 */
data class SearchSection(
    val module: SearchModule,
    val entries: List<IndicatorEntry>,
)
