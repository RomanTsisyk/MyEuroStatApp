package eu.eurostat.feature.search.ui

import eu.eurostat.feature.search.domain.IndicatorEntry
import eu.eurostat.feature.search.domain.SearchSection

/**
 * Sealed UI state for the Search screen.
 *
 * There is only a [Content] variant — no Loading/Empty/Error: the indicator
 * index is a compiled-in Kotlin list and ranking is a pure synchronous
 * function, so there is nothing to load and nothing that can fail. The
 * "no results" presentation is derived in the screen from a non-blank
 * [Content.query] with empty [Content.results].
 */
sealed interface SearchUiState {

    /**
     * @property query the raw text currently in the search field.
     * @property results ranked matches for [query], best first; empty when
     *   the query is blank (browse mode) or nothing matched.
     * @property browseSections all indicators grouped by module in canonical
     *   order; constant for the lifetime of the screen and rendered whenever
     *   [query] is blank.
     */
    data class Content(
        val query: String,
        val results: List<IndicatorEntry>,
        val browseSections: List<SearchSection>,
    ) : SearchUiState
}
