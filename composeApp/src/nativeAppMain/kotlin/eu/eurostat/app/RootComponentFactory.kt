package eu.eurostat.app

import com.arkivanov.decompose.ComponentContext
import eu.eurostat.core.navigation.ChildConfig
import eu.eurostat.core.navigation.ComponentFactory
import eu.eurostat.core.navigation.DefaultRootComponent
import eu.eurostat.core.navigation.RootComponent
import org.koin.core.qualifier.named
import org.koin.mp.KoinPlatform.getKoin

fun createRootComponent(componentContext: ComponentContext): RootComponent {
    val koin = getKoin()
    // ChildConfig.Home resolves to the Overview dashboard component (feature-overview).
    val factories: Map<String, ComponentFactory<Any>> = listOf(
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
        val key = cls.qualifiedName!!
        key to koin.get<ComponentFactory<Any>>(qualifier = named(key))
    }
    return DefaultRootComponent(componentContext, factories)
}
