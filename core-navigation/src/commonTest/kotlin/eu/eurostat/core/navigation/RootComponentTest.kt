package eu.eurostat.core.navigation

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.resume
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Navigation-behavior tests for [DefaultRootComponent]: the initial destination,
 * factory-backed child creation, tab switching (bring-to-front) and back handling.
 *
 * Every destination — including Home (which resolves to the Overview dashboard) —
 * is stubbed with a simple sentinel string via a fake [ComponentFactory], so the
 * test needs no real feature modules.
 */
class RootComponentTest {

    private val lifecycle = LifecycleRegistry()
    private val context = DefaultComponentContext(lifecycle)

    private fun factoryReturning(value: Any): ComponentFactory<Any> = ComponentFactory { value }

    private val factories: Map<String, ComponentFactory<Any>> = mapOf(
        requireNotNull(ChildConfig.Home::class.qualifiedName) to factoryReturning("Home"),
        requireNotNull(ChildConfig.Economy::class.qualifiedName) to factoryReturning("Economy"),
        requireNotNull(ChildConfig.Population::class.qualifiedName) to factoryReturning("Population"),
        requireNotNull(ChildConfig.Settings::class.qualifiedName) to factoryReturning("Settings"),
        requireNotNull(ChildConfig.Search::class.qualifiedName) to factoryReturning("Search"),
    )

    @BeforeTest
    fun resume() {
        lifecycle.resume()
    }

    @AfterTest
    fun destroy() {
        lifecycle.destroy()
    }

    private fun build() = DefaultRootComponent(context, factories)

    private fun RootComponent.configs() = stack.value.items.map { it.configuration }

    @Test
    fun initial_destination_is_home_backed_by_its_factory() {
        val root = build()
        assertEquals(ChildConfig.Home, root.stack.value.active.configuration)
        assertEquals("Home", root.stack.value.active.instance)
    }

    @Test
    fun selecting_a_tab_brings_its_factory_backed_child_to_front() {
        val root = build()
        root.onTabSelected(ChildConfig.Economy)

        val active = root.stack.value.active
        assertEquals(ChildConfig.Economy, active.configuration)
        assertEquals("Economy", active.instance)
    }

    @Test
    fun back_from_a_feature_returns_to_home() {
        val root = build()
        root.onTabSelected(ChildConfig.Economy)
        root.onBack()
        assertEquals(ChildConfig.Home, root.stack.value.active.configuration)
    }

    @Test
    fun back_pops_only_the_top_of_the_stack() {
        val root = build()
        root.onTabSelected(ChildConfig.Population)
        root.onTabSelected(ChildConfig.Economy)
        // Stack is now [Home, Population, Economy]; popping returns to Population.
        root.onBack()
        assertEquals(ChildConfig.Population, root.stack.value.active.configuration)
    }

    @Test
    fun search_opened_from_a_module_header_returns_to_that_module_on_back() {
        val root = build()
        root.onTabSelected(ChildConfig.Population)
        // Module header search icon: pushes Search on top of the current module.
        root.onTabSelected(ChildConfig.Search)
        assertEquals(ChildConfig.Search, root.stack.value.active.configuration)
        assertEquals("Search", root.stack.value.active.instance)

        root.onBack()
        assertEquals(ChildConfig.Population, root.stack.value.active.configuration)
    }

    @Test
    fun search_result_opened_from_a_module_replaces_search_and_back_returns_to_that_module() {
        val root = build()
        root.onTabSelected(ChildConfig.Population)
        root.onTabSelected(ChildConfig.Search)
        // Picking a result in Search navigates to the target module.
        root.onTabSelected(ChildConfig.Economy)

        assertEquals(ChildConfig.Economy, root.stack.value.active.configuration)
        assertEquals(
            listOf(ChildConfig.Home, ChildConfig.Population, ChildConfig.Economy),
            root.configs(),
        )

        root.onBack()
        assertEquals(ChildConfig.Population, root.stack.value.active.configuration)
    }

    @Test
    fun search_result_opened_from_overview_returns_to_home_on_back() {
        val root = build()
        root.onTabSelected(ChildConfig.Search)
        root.onTabSelected(ChildConfig.Economy)

        assertEquals(listOf(ChildConfig.Home, ChildConfig.Economy), root.configs())

        root.onBack()
        assertEquals(ChildConfig.Home, root.stack.value.active.configuration)
    }

    @Test
    fun search_result_in_the_module_the_user_came_from_leaves_a_single_copy() {
        val root = build()
        root.onTabSelected(ChildConfig.Population)
        root.onTabSelected(ChildConfig.Search)
        root.onTabSelected(ChildConfig.Population)

        assertEquals(listOf(ChildConfig.Home, ChildConfig.Population), root.configs())

        root.onBack()
        assertEquals(ChildConfig.Home, root.stack.value.active.configuration)
    }

    @Test
    fun search_can_be_reopened_after_a_result_without_duplicating_it() {
        val root = build()
        root.onTabSelected(ChildConfig.Population)
        root.onTabSelected(ChildConfig.Search)
        root.onTabSelected(ChildConfig.Economy)
        root.onTabSelected(ChildConfig.Search)

        assertEquals(ChildConfig.Search, root.stack.value.active.configuration)
        assertEquals(1, root.configs().count { it == ChildConfig.Search })

        root.onBack()
        assertEquals(ChildConfig.Economy, root.stack.value.active.configuration)
    }
}
