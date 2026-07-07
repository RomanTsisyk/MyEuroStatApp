package eu.eurostat.feature.search.domain

/**
 * One searchable indicator shipped by a feature module.
 *
 * Entries are static metadata (see [SearchIndex]); they describe what a module
 * shows, not live data, so the Search screen needs no network or cache.
 *
 * @property id stable unique identifier, kebab-case (e.g. "hicp-inflation").
 * @property label user-facing indicator name shown as the result headline.
 * @property module owning feature module; provides accent color and the
 *   navigation destination.
 * @property datasetCode Eurostat dataset code (e.g. "prc_hicp_aind"), rendered
 *   in monospace as provenance.
 * @property description one-line summary of what the indicator measures.
 * @property keywords lowercase natural-language synonyms used for matching
 *   (e.g. "inflation", "co2", "hotels"); not shown in the UI.
 */
data class IndicatorEntry(
    val id: String,
    val label: String,
    val module: SearchModule,
    val datasetCode: String,
    val description: String,
    val keywords: List<String>,
)
