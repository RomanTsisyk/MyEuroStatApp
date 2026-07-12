package eu.eurostat.feature.search.domain

import eu.eurostat.core.navigation.ChildConfig
import myeurostatapp.feature_search.generated.resources.Res
import myeurostatapp.feature_search.generated.resources.search_module_economy
import myeurostatapp.feature_search.generated.resources.search_module_environment
import myeurostatapp.feature_search.generated.resources.search_module_population
import myeurostatapp.feature_search.generated.resources.search_module_science
import myeurostatapp.feature_search.generated.resources.search_module_social
import myeurostatapp.feature_search.generated.resources.search_module_tourism
import myeurostatapp.feature_search.generated.resources.search_module_trade
import myeurostatapp.feature_search.generated.resources.search_module_transport
import org.jetbrains.compose.resources.StringResource

/**
 * The eight feature modules an indicator can belong to.
 *
 * Each value carries everything the Search screen needs to render and route a
 * result: a localized [titleRes], the [accentKey] resolved through
 * `Euro.moduleAccents.forModule()`, and the [destination] pushed on the
 * Decompose stack when a result is opened.
 *
 * [titleRes] is owned by feature-search itself (not the corresponding
 * feature-* module's own `*_module_title`) because this module does not
 * depend on the other seven feature modules and cannot resolve their
 * Compose Resources — see `search_module_*` in this module's `strings.xml`.
 *
 * Declaration order is the canonical browse order (matches the Overview grid).
 */
enum class SearchModule(
    /** Localized module name, e.g. "Population" / "Ludność" / "Населення". */
    val titleRes: StringResource,
    /** Key understood by `ModuleAccents.forModule()` (e.g. "people", "climate"). */
    val accentKey: String,
    /** Navigation target for indicators in this module. */
    val destination: ChildConfig,
) {
    POPULATION(Res.string.search_module_population, "people", ChildConfig.Population),
    ECONOMY(Res.string.search_module_economy, "economy", ChildConfig.Economy),
    ENVIRONMENT(Res.string.search_module_environment, "climate", ChildConfig.Environment),
    TRADE(Res.string.search_module_trade, "trade", ChildConfig.Trade),
    TRANSPORT(Res.string.search_module_transport, "transport", ChildConfig.Transport),
    TOURISM(Res.string.search_module_tourism, "tourism", ChildConfig.Tourism),
    SOCIAL(Res.string.search_module_social, "social", ChildConfig.Social),
    SCIENCE(Res.string.search_module_science, "science", ChildConfig.Science),
}
