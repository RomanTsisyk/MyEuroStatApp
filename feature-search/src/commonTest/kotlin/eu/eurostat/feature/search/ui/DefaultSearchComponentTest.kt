package eu.eurostat.feature.search.ui

import app.cash.turbine.test
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.resume
import eu.eurostat.core.navigation.ChildConfig
import eu.eurostat.feature.search.domain.SearchIndex
import eu.eurostat.feature.search.domain.SearchModule
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Round-trip tests for [DefaultSearchComponent]: initial browse state,
 * SetQuery re-ranking, and OpenResult routing to the navigation callback.
 * The component is fully synchronous, so no test dispatcher is required.
 */
class DefaultSearchComponentTest {

    private val lifecycle = LifecycleRegistry()
    private val context = DefaultComponentContext(lifecycle)

    /** Records every config the component asked the root to navigate to. */
    private val navigatedTo = mutableListOf<ChildConfig>()

    @BeforeTest
    fun resume() {
        lifecycle.resume()
    }

    @AfterTest
    fun destroy() {
        lifecycle.destroy()
    }

    private fun build() = DefaultSearchComponent(
        componentContext = context,
        onNavigateToModule = { navigatedTo += it },
    )

    private fun content(component: SearchComponent): SearchUiState.Content =
        assertIs<SearchUiState.Content>(component.state.value)

    @Test
    fun initial_state_is_blank_query_with_all_modules_browsable() {
        val state = content(build())

        assertEquals("", state.query)
        assertEquals(emptyList(), state.results)
        assertEquals(
            SearchModule.entries.toList(),
            state.browseSections.map { it.module },
            "browse must list every module in canonical order",
        )
        assertEquals(
            SearchIndex.entries.size,
            state.browseSections.sumOf { it.entries.size },
            "browse must cover the whole index",
        )
    }

    @Test
    fun set_query_updates_query_and_ranks_results() = runTest {
        val component = build()

        component.state.test {
            assertEquals("", assertIs<SearchUiState.Content>(awaitItem()).query)

            component.onIntent(SearchIntent.SetQuery("inflation"))

            val updated = assertIs<SearchUiState.Content>(awaitItem())
            assertEquals("inflation", updated.query)
            assertEquals("hicp-inflation", updated.results.first().id)
        }
    }

    @Test
    fun clearing_the_query_returns_to_browse_mode() {
        val component = build()

        component.onIntent(SearchIntent.SetQuery("co2"))
        assertTrue(content(component).results.isNotEmpty())

        component.onIntent(SearchIntent.SetQuery(""))
        val state = content(component)
        assertEquals("", state.query)
        assertEquals(emptyList(), state.results)
        assertTrue(state.browseSections.isNotEmpty(), "browse sections stay available")
    }

    @Test
    fun unmatched_query_keeps_the_query_but_yields_no_results() {
        val component = build()

        component.onIntent(SearchIntent.SetQuery("zzzz"))

        val state = content(component)
        assertEquals("zzzz", state.query)
        assertEquals(emptyList(), state.results)
    }

    @Test
    fun open_result_navigates_to_the_owning_module() {
        val component = build()
        component.onIntent(SearchIntent.SetQuery("inflation"))
        val hit = content(component).results.first()

        component.onIntent(SearchIntent.OpenResult(hit))

        assertEquals(listOf<ChildConfig>(ChildConfig.Economy), navigatedTo)
    }

    @Test
    fun open_result_routes_each_module_to_its_own_destination() {
        val component = build()
        val oneEntryPerModule = SearchModule.entries.map { module ->
            SearchIndex.entries.first { it.module == module }
        }

        oneEntryPerModule.forEach { component.onIntent(SearchIntent.OpenResult(it)) }

        assertEquals(SearchModule.entries.map { it.destination }, navigatedTo)
    }
}
