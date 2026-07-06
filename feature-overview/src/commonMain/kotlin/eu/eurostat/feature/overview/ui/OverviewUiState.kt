package eu.eurostat.feature.overview.ui

import eu.eurostat.core.navigation.ChildConfig

/** Load state of a single module teaser on the Overview dashboard. */
enum class TeaserStatus {
    /** The underlying repository is still loading (no cache, no fresh data yet). */
    Loading,

    /** A headline value was resolved for the default country. */
    Loaded,

    /** The dataset returned successfully but had no usable headline value. */
    Empty,

    /** The underlying repository emitted an error and no cache was available. */
    Error,
}

/**
 * One module's at-a-glance card on the Overview dashboard.
 *
 * @property destination the navigation target opened when the tile is tapped.
 * @property accentKey key passed to `Euro.moduleAccents.forModule(...)`.
 * @property title user-facing module name.
 * @property emoji leading glyph shown on the tile.
 * @property value formatted headline value for the default country (or `"—"`).
 * @property unit short qualifier for [value] (e.g. `"people"`, `"B € · GDP"`).
 * @property year the year [value] belongs to, or null when unknown.
 * @property status current load state.
 */
data class ModuleTeaser(
    val destination: ChildConfig,
    val accentKey: String,
    val title: String,
    val emoji: String,
    val value: String,
    val unit: String,
    val year: Int?,
    val status: TeaserStatus,
)

/**
 * Immutable snapshot of the Overview dashboard: one [ModuleTeaser] per feature,
 * in display order. The dashboard never fails as a whole — individual teasers
 * carry their own [TeaserStatus], so a single broken dataset degrades one tile
 * instead of the entire landing screen.
 */
data class OverviewUiState(
    val teasers: List<ModuleTeaser>,
) {
    /** The headline module shown in the hero block (Economy · GDP). */
    val hero: ModuleTeaser?
        get() = teasers.firstOrNull { it.destination == ChildConfig.Economy }
}
