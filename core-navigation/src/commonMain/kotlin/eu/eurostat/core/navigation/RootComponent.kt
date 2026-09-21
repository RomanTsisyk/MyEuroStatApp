package eu.eurostat.core.navigation

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.navigate
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.value.Value

/**
 * Root of the app's navigation: owns the Decompose back stack of [ChildConfig] destinations.
 *
 * [ChildConfig.Search] is a transient overlay, not a regular destination: it is pushed on top of
 * whatever screen opened it and never survives navigating away from it. Picking a search result
 * therefore replaces Search with the target module, so Back returns to where Search was opened
 * from rather than to Search.
 */
interface RootComponent {
    /** The current back stack; the last entry is the active destination. */
    val stack: Value<ChildStack<ChildConfig, Any>>

    /**
     * Brings [config] to the front of the stack, dropping any earlier entry of the same
     * destination type so each destination appears at most once.
     *
     * Search rule: [ChildConfig.Search] is kept only while it is the top entry. Navigating to any
     * other destination first removes Search from the stack, so a result chosen in Search leaves
     * `[..., target]` behind and Back returns to where Search was opened from. Navigating to
     * [ChildConfig.Search] itself simply pushes it on top of the current destination.
     */
    fun onTabSelected(config: ChildConfig)

    /**
     * Pops the top entry of the stack. Back from [ChildConfig.Search] returns to the destination
     * it was opened from.
     */
    fun onBack()
}

class DefaultRootComponent(
    componentContext: ComponentContext,
    private val factories: Map<String, ComponentFactory<Any>>,
) : RootComponent, ComponentContext by componentContext {

    private val navigation = StackNavigation<ChildConfig>()

    override val stack: Value<ChildStack<ChildConfig, Any>> = childStack(
        source = navigation,
        serializer = ChildConfig.serializer(),
        initialConfiguration = ChildConfig.Home,
        handleBackButton = true,
        childFactory = ::createChild,
    )

    private fun createChild(config: ChildConfig, ctx: ComponentContext): Any {
        val key = config::class.qualifiedName ?: error("config without qualified name")
        val factory = factories[key] ?: error("no ComponentFactory registered for $key")
        return factory.create(ctx)
    }

    override fun onTabSelected(config: ChildConfig) {
        navigation.navigate { current ->
            // bringToFront semantics (drop same-class entries, append config) plus:
            // Search is a transient overlay and never survives navigating away from it.
            current.filterNot { it == ChildConfig.Search || it::class == config::class } + config
        }
    }

    override fun onBack() {
        navigation.pop()
    }
}
