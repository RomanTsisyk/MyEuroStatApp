package eu.eurostat.app

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.stack.Children
import eu.eurostat.app.locale.AppLocaleEnvironment
import eu.eurostat.core.common.prefs.AppPreferences
import eu.eurostat.core.common.prefs.ThemePreference
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
import eu.eurostat.feature.search.ui.SearchComponent
import eu.eurostat.feature.search.ui.SearchScreen
import eu.eurostat.feature.settings.ui.SettingsComponent
import eu.eurostat.feature.settings.ui.SettingsScreen
import eu.eurostat.feature.transport.ui.TransportComponent
import eu.eurostat.feature.transport.ui.TransportScreen
import eu.eurostat.ui.layout.AdaptiveScaffold
import eu.eurostat.ui.theme.Euro
import eu.eurostat.ui.theme.EurostatTheme
import org.koin.mp.KoinPlatform

/**
 * Root composable. Hosts a Home grid screen as the default destination.
 * Tapping a module card pushes the corresponding feature screen via Decompose.
 * Each feature screen's back button pops back to Home via [RootComponent.onBack].
 *
 * Observes the persisted theme preference ([AppPreferences.themePreference])
 * and re-themes live when the user changes it on the Settings screen;
 * [ThemePreference.SYSTEM] follows [isSystemInDarkTheme]. The persisted
 * language preference ([AppPreferences.language]) is applied the same way:
 * [AppLocaleEnvironment] overrides the resource locale app-wide, so picking
 * a language in Settings re-renders every screen in it immediately
 * (`"system"` follows the OS locale). Koin is resolved lazily here because
 * every platform entry point starts Koin before composing this function.
 *
 * The [BottomTabBar] component file is preserved for potential future use but
 * is no longer rendered here.
 */
@Composable
fun EurostatApp(root: RootComponent) {
    val preferences = remember { KoinPlatform.getKoin().get<AppPreferences>() }
    val themePreference by preferences.themePreference
        .collectAsState(initial = ThemePreference.SYSTEM)
    val darkTheme = when (themePreference) {
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
    }
    val language by preferences.language
        .collectAsState(initial = AppPreferences.DEFAULT_LANGUAGE)
    val languageTag = language.takeUnless { it == AppPreferences.DEFAULT_LANGUAGE }
    AppLocaleEnvironment(languageTag) {
        AppContent(root = root, darkTheme = darkTheme)
    }
}

/**
 * Themed navigation shell, split out of [EurostatApp] so the locale wrapper
 * has a single child to rebuild on language switches.
 */
@Composable
private fun AppContent(root: RootComponent, darkTheme: Boolean) {
    EurostatTheme(darkTheme = darkTheme) {
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
                    is SearchComponent      -> SearchScreen(
                        component = c,
                        onBack = { root.onBack() },
                    )
                    else -> error("Unknown child instance: $c")
                }
            }
        }
    }
}
