package eu.eurostat.app

import com.arkivanov.decompose.ComponentContext
import eu.eurostat.core.navigation.ChildConfig
import eu.eurostat.core.navigation.ComponentFactory
import eu.eurostat.core.navigation.DefaultRootComponent
import eu.eurostat.core.navigation.RootComponent
import eu.eurostat.feature.search.ui.DefaultSearchComponent
import org.koin.core.qualifier.named
import org.koin.mp.KoinPlatform.getKoin

fun createRootComponent(componentContext: ComponentContext): RootComponent {
    val koin = getKoin()
    // ChildConfig.Home resolves to the Overview dashboard component (feature-overview).
    val koinFactories: Map<String, ComponentFactory<Any>> = listOf(
        ChildConfig.Home::class,
        ChildConfig.Population::class,
        ChildConfig.Economy::class,
        ChildConfig.Environment::class,
        ChildConfig.Trade::class,
        ChildConfig.Transport::class,
        ChildConfig.Tourism::class,
        ChildConfig.Social::class,
        ChildConfig.Science::class,
        ChildConfig.Settings::class,
    ).associate { cls ->
        val key = requireNotNull(cls.qualifiedName)
        key to koin.get<ComponentFactory<Any>>(qualifier = named(key))
    }

    // Search is wired here rather than through Koin: DefaultSearchComponent
    // takes an onNavigateToModule callback into the RootComponent, which does
    // not exist yet when the Koin modules are assembled. The root reference is
    // late-bound — it is assigned below, before any UI (and therefore any
    // search result tap) can invoke the callback. Opening a result pushes the
    // target module on top of Search, so back returns to the results.
    var root: RootComponent? = null
    val searchKey = requireNotNull(ChildConfig.Search::class.qualifiedName)
    val searchFactory = ComponentFactory<Any> { ctx ->
        DefaultSearchComponent(
            componentContext = ctx,
            onNavigateToModule = { config ->
                checkNotNull(root) { "RootComponent not initialised yet" }.onTabSelected(config)
            },
        )
    }

    return DefaultRootComponent(componentContext, koinFactories + (searchKey to searchFactory))
        .also { root = it }
}
