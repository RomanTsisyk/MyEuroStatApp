package eu.eurostat.feature.search.domain

import eu.eurostat.core.navigation.ChildConfig

/**
 * The eight feature modules an indicator can belong to.
 *
 * Each value carries everything the Search screen needs to render and route a
 * result: a user-facing [displayName], the [accentKey] resolved through
 * `Euro.moduleAccents.forModule()`, and the [destination] pushed on the
 * Decompose stack when a result is opened.
 *
 * Declaration order is the canonical browse order (matches the Overview grid).
 */
enum class SearchModule(
    /** User-facing module name, e.g. "Population". */
    val displayName: String,
    /** Key understood by `ModuleAccents.forModule()` (e.g. "people", "climate"). */
    val accentKey: String,
    /** Navigation target for indicators in this module. */
    val destination: ChildConfig,
) {
    POPULATION("Population", "people", ChildConfig.Population),
    ECONOMY("Economy", "economy", ChildConfig.Economy),
    ENVIRONMENT("Environment", "climate", ChildConfig.Environment),
    TRADE("Trade", "trade", ChildConfig.Trade),
    TRANSPORT("Transport", "transport", ChildConfig.Transport),
    TOURISM("Tourism", "tourism", ChildConfig.Tourism),
    SOCIAL("Social", "social", ChildConfig.Social),
    SCIENCE("Science", "science", ChildConfig.Science),
}
