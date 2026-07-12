package eu.eurostat.feature.search.ui

import com.arkivanov.decompose.ComponentContext
import eu.eurostat.core.navigation.ChildConfig
import eu.eurostat.feature.search.domain.IndicatorEntry
import eu.eurostat.feature.search.domain.SearchIndex
import eu.eurostat.feature.search.domain.browseSections
import eu.eurostat.feature.search.domain.rankIndicators
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Decompose component for the Search screen: holds the query, ranks the
 * static indicator index against it, and routes opened results to the owning
 * feature module.
 */
interface SearchComponent {
    val state: StateFlow<SearchUiState>
    fun onIntent(intent: SearchIntent)
}

/**
 * Default [SearchComponent].
 *
 * Fully synchronous — the index is an in-memory list and ranking is a pure
 * function, so no coroutine scope, dispatcher, or repository is needed.
 *
 * @param componentContext Decompose lifecycle owner (delegated).
 * @param onNavigateToModule invoked with the destination [ChildConfig] when a
 *   result is opened; the root component pushes it on the navigation stack.
 * @param index the searchable entries; defaults to [SearchIndex.entries] and
 *   is injectable for tests.
 */
class DefaultSearchComponent(
    componentContext: ComponentContext,
    private val onNavigateToModule: (ChildConfig) -> Unit,
    private val index: List<IndicatorEntry> = SearchIndex.entries,
) : SearchComponent, ComponentContext by componentContext {

    // Internally typed as Content (the only variant); exposed as the sealed
    // supertype so the screen dispatches on SearchUiState like every module.
    private val _state = MutableStateFlow(
        SearchUiState.Content(
            query = "",
            results = emptyList(),
            browseSections = browseSections(index),
        ),
    )
    override val state: StateFlow<SearchUiState> = _state.asStateFlow()

    override fun onIntent(intent: SearchIntent) {
        when (intent) {
            is SearchIntent.SetQuery -> setQuery(intent.query)
            is SearchIntent.OpenResult -> onNavigateToModule(intent.entry.module.destination)
        }
    }

    private fun setQuery(query: String) {
        _state.value = _state.value.copy(
            query = query,
            results = rankIndicators(query, index),
        )
    }
}
