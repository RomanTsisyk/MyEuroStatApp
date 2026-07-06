package eu.eurostat.feature.overview.di

import eu.eurostat.core.navigation.ChildConfig
import eu.eurostat.core.navigation.ComponentFactory
import eu.eurostat.feature.overview.ui.DefaultOverviewComponent
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * Registers the Overview component factory under [ChildConfig.Home], so the
 * landing destination resolves to the data-rich dashboard. The eight feature
 * repositories are injected as Koin singletons (shared with the feature screens,
 * so no duplicate network calls).
 */
fun featureOverviewModule() = module {
    factory<ComponentFactory<Any>>(
        qualifier = named(ChildConfig.Home::class.qualifiedName!!),
    ) {
        ComponentFactory { ctx ->
            DefaultOverviewComponent(
                ctx,
                get(), // PopulationRepository
                get(), // EconomyRepository
                get(), // EnvironmentRepository
                get(), // TradeRepository
                get(), // TransportRepository
                get(), // TourismRepository
                get(), // SocialRepository
                get(), // ScienceRepository
                get(), // DispatcherProvider
            )
        }
    }
}
