package eu.eurostat.feature.compare.di

import eu.eurostat.core.navigation.ChildConfig
import eu.eurostat.core.navigation.ComponentFactory
import eu.eurostat.feature.compare.data.CompareDataSource
import eu.eurostat.feature.compare.ui.DefaultCompareComponent
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * Registers the Compare component factory under [ChildConfig.Compare], so the
 * cross-module compare destination resolves to [DefaultCompareComponent].
 *
 * The [CompareDataSource] is bound here from the eight feature repositories
 * (injected as Koin singletons — shared with the feature screens and Overview,
 * so no duplicate network calls). Constructor argument order matches
 * [CompareDataSource]: population, economy, environment, trade, transport,
 * tourism, social, science.
 */
fun featureCompareModule() = module {
    factory {
        CompareDataSource(
            get(), // PopulationRepository
            get(), // EconomyRepository
            get(), // EnvironmentRepository
            get(), // TradeRepository
            get(), // TransportRepository
            get(), // TourismRepository
            get(), // SocialRepository
            get(), // ScienceRepository
        )
    }
    factory<ComponentFactory<Any>>(
        qualifier = named(ChildConfig.Compare::class.qualifiedName!!),
    ) {
        ComponentFactory { ctx ->
            DefaultCompareComponent(
                ctx,
                get(), // CompareDataSource
                get(), // DispatcherProvider
                get(), // AppPreferences
            )
        }
    }
}
