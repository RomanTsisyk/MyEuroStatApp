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
}
