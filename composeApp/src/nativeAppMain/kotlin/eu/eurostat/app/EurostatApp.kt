package eu.eurostat.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.stack.Children
import eu.eurostat.core.navigation.ChildConfig
import eu.eurostat.core.navigation.RootComponent
import eu.eurostat.feature.economy.ui.EconomyComponent
import eu.eurostat.feature.economy.ui.EconomyScreen
import eu.eurostat.feature.environment.ui.EnvironmentComponent
import eu.eurostat.feature.environment.ui.EnvironmentScreen
import eu.eurostat.feature.population.ui.PopulationComponent
import eu.eurostat.feature.population.ui.PopulationScreen
import eu.eurostat.feature.science.ui.ScienceComponent
import eu.eurostat.feature.science.ui.ScienceScreen
import eu.eurostat.feature.overview.ui.OverviewComponent
import eu.eurostat.feature.overview.ui.OverviewScreen
import eu.eurostat.feature.social.ui.SocialComponent
import eu.eurostat.feature.social.ui.SocialScreen
import eu.eurostat.feature.tourism.ui.TourismComponent
import eu.eurostat.feature.tourism.ui.TourismScreen
import eu.eurostat.feature.trade.ui.TradeComponent
import eu.eurostat.feature.trade.ui.TradeScreen
import eu.eurostat.feature.settings.ui.SettingsComponent
import eu.eurostat.feature.settings.ui.SettingsScreen
import eu.eurostat.feature.transport.ui.TransportComponent
import eu.eurostat.feature.transport.ui.TransportScreen
import eu.eurostat.ui.layout.AdaptiveScaffold
import eu.eurostat.ui.theme.Euro
import eu.eurostat.ui.theme.EurostatTheme

/**
 * Root composable. Hosts a Home grid screen as the default destination.
 * Tapping a module card pushes the corresponding feature screen via Decompose.
 * Each feature screen's back button pops back to Home via [RootComponent.onBack].
 *
 * The [BottomTabBar] component file is preserved for potential future use but
 * is no longer rendered here.
 */
@Composable
fun EurostatApp(root: RootComponent) {
    EurostatTheme {
        AdaptiveScaffold(modifier = Modifier.fillMaxSize()) { _ ->
            Children(
                stack = root.stack,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Euro.colors.paper),
            ) { child ->
                when (val c = child.instance) {
                    // Home resolves to the Overview dashboard aggregator component.
                    is OverviewComponent -> OverviewScreen(
                        component = c,
                        onModuleSelected = { config -> root.onTabSelected(config) },
                        modifier = Modifier.fillMaxSize(),
                    )
                    is PopulationComponent  -> PopulationScreen(
                        component = c,
                        onBack = { root.onBack() },
                    )
                    is EconomyComponent     -> EconomyScreen(
                        component = c,
                        onBack = { root.onBack() },
                    )
                    is EnvironmentComponent -> EnvironmentScreen(
                        component = c,
                        onBack = { root.onBack() },
                    )
                    is TradeComponent       -> TradeScreen(
                        component = c,
                        onBack = { root.onBack() },
                    )
                    is TransportComponent   -> TransportScreen(
                        component = c,
                        onBack = { root.onBack() },
                    )
                    is TourismComponent     -> TourismScreen(
                        component = c,
                        onBack = { root.onBack() },
                    )
                    is SocialComponent      -> SocialScreen(
                        component = c,
                        onBack = { root.onBack() },
                    )
                    is ScienceComponent     -> ScienceScreen(
                        component = c,
                        onBack = { root.onBack() },
                    )
                    is SettingsComponent    -> SettingsScreen(
                        component = c,
                        onBack = { root.onBack() },
                    )
                    else -> error("Unknown child instance: $c")
                }
            }
        }
    }
}
