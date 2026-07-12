package eu.eurostat.feature.search.ui

import eu.eurostat.feature.search.domain.IndicatorEntry

/** User actions on the Search screen. */
sealed interface SearchIntent {

    /** Replace the query text and re-rank results synchronously. */
    data class SetQuery(val query: String) : SearchIntent

    /** Open the feature module that ships [entry]. */
    data class OpenResult(val entry: IndicatorEntry) : SearchIntent
}
