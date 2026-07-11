package eu.eurostat.feature.overview.ui

import eu.eurostat.core.navigation.ChildConfig
import org.jetbrains.compose.resources.StringResource

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
 * [titleRes] and [unitRes] are Compose string resources rather than plain
 * [String]s because this type is built in [DefaultOverviewComponent], which is
 * not `@Composable` and therefore cannot call `stringResource()`; resolution
 * happens where the teaser is actually rendered (`TeaserTile` in
 * `OverviewScreen.kt`). See `feature-search`'s `SearchModule.titleRes` for
 * the same pattern.
 *
 * @property destination the navigation target opened when the tile is tapped.
 * @property accentKey key passed to `Euro.moduleAccents.forModule(...)`.
 * @property titleRes localized user-facing module name (e.g. "Population" / "Ludność" / "Населення").
 * @property emoji leading glyph shown on the tile.
 * @property value formatted headline value for the default country (or `"—"`).
 * @property unitRes localized short qualifier for [value] (e.g. `"people"`, `"B € · GDP"`).
 * @property year the year [value] belongs to, or null when unknown.
 * @property status current load state.
 */
data class ModuleTeaser(
    val destination: ChildConfig,
    val accentKey: String,
    val titleRes: StringResource,
    val emoji: String,
    val value: String,
    val unitRes: StringResource,
    val year: Int?,
    val status: TeaserStatus,
)

/**
 * Immutable snapshot of the Overview dashboard: one [ModuleTeaser] per feature,
 * in display order. The dashboard never fails as a whole — individual teasers
 * carry their own [TeaserStatus], so a single broken dataset degrades one tile
 * instead of the entire landing screen.
 *
 * @property headlineCountryCode the Eurostat country code the hero block headlines
 *   (the persisted default-country preference, or [DefaultOverviewComponent.DEFAULT_COUNTRY]).
 *   Interpolated into `overview_hero_subtitle` instead of a fixed country name.
 */
data class OverviewUiState(
    val teasers: List<ModuleTeaser>,
    val headlineCountryCode: String,
) {
    /** The headline module shown in the hero block (Economy · GDP). */
    val hero: ModuleTeaser?
        get() = teasers.firstOrNull { it.destination == ChildConfig.Economy }
}
