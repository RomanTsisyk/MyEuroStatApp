package eu.eurostat.core.navigation

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.bringToFront
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.value.Value

interface RootComponent {
    val stack: Value<ChildStack<ChildConfig, Any>>
    fun onTabSelected(config: ChildConfig)
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
        navigation.bringToFront(config)
    }

    override fun onBack() {
        navigation.pop()
    }
}
