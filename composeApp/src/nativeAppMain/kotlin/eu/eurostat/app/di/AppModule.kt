package eu.eurostat.app.di

import eu.eurostat.core.common.di.coreCommonModule
import eu.eurostat.core.database.di.coreDatabaseModule
import eu.eurostat.core.navigation.di.coreNavigationModule
import eu.eurostat.core.network.di.coreNetworkModule
import eu.eurostat.feature.economy.di.featureEconomyModule
import eu.eurostat.feature.environment.di.featureEnvironmentModule
import eu.eurostat.feature.population.di.featurePopulationModule
import eu.eurostat.feature.science.di.featureScienceModule
import eu.eurostat.feature.settings.di.featureSettingsModule
import eu.eurostat.feature.overview.di.featureOverviewModule
import eu.eurostat.feature.social.di.featureSocialModule
import eu.eurostat.feature.tourism.di.featureTourismModule
import eu.eurostat.feature.trade.di.featureTradeModule
import eu.eurostat.feature.transport.di.featureTransportModule
import org.koin.core.module.Module

fun appModules(): List<Module> = listOf(
    coreCommonModule(),
    coreNetworkModule(),
    coreDatabaseModule(),
    coreNavigationModule(),
    featurePopulationModule(),
    featureEconomyModule(),
    featureEnvironmentModule(),
    featureTradeModule(),
    featureTransportModule(),
    featureTourismModule(),
    featureSocialModule(),
    featureScienceModule(),
    featureSettingsModule(),
    featureOverviewModule(),
)
