package eu.eurostat.feature.overview.ui

import eu.eurostat.core.common.AppError
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
 * How a [ModuleTeaser.value] is rendered to text by [formatTeaserValue].
 *
 * The UI state deliberately carries the raw number plus one of these
 * discriminators instead of a pre-formatted string: number formatting depends
 * on the runtime app locale, which can change while the (singleton)
 * component is alive, so it must happen at composition time.
 */
enum class TeaserFormat {
    /** Compact K/M/B count of a raw quantity (people, nights, passengers), e.g. `83.5M`. */
    Compact,

    /** Amount reported in millions of EUR, shown as whole, grouped billions, e.g. `4,387`. */
    BillionsFromMillions,

    /** Plain whole number with digit grouping (e.g. megatonnes CO2-eq), e.g. `650`. */
    Grouped,

    /** Percentage with one decimal digit, e.g. `15.5%`. */
    Percent,
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
 * For the same reason [value] is the *raw* number, not text: the screen turns
 * it into a string with [formatTeaserValue] while composing, so it always
 * follows the locale currently in effect (a runtime language switch must not
 * leave previously formatted strings behind).
 *
 * @property destination the navigation target opened when the tile is tapped.
 * @property accentKey key passed to `Euro.moduleAccents.forModule(...)`.
 * @property titleRes localized user-facing module name (e.g. "Population" / "Ludność" / "Населення").
 * @property emoji leading glyph shown on the tile.
 * @property value raw headline value for the headline country in the source unit
 *   of the dataset (people, EUR millions, Mt CO2-eq, percent, ...), or null while
 *   there is nothing to show (loading, empty, error). Interpreted per [format].
 * @property format how [value] is rendered; fixed per module.
 * @property unitRes localized short qualifier for [value] (e.g. `"people"`, `"B € · GDP"`).
 * @property year the year [value] belongs to, or null when unknown.
 * @property status current load state.
 * @property error the cause when [status] is [TeaserStatus.Error], else null.
 */
data class ModuleTeaser(
    val destination: ChildConfig,
    val accentKey: String,
    val titleRes: StringResource,
    val emoji: String,
    val value: Double?,
    val format: TeaserFormat,
    val unitRes: StringResource,
    val year: Int?,
    val status: TeaserStatus,
    val error: AppError? = null,
)

/**
 * Immutable snapshot of the Overview dashboard: one [ModuleTeaser] per feature,
 * in display order. The dashboard never fails as a whole — individual teasers
 * carry their own [TeaserStatus], so a single broken dataset degrades one tile
 * instead of the entire landing screen. The one exception worth telling the user
 * about is total failure (every tile settled without a value, at least one with an
 * error, e.g. offline with an empty cache), which is surfaced via [unavailableError].
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

    /**
     * The cause to show as a dashboard-level hint when nothing could be loaded, else null.
     *
     * Non-null only when every teaser has settled without a value (none
     * [TeaserStatus.Loading], none [TeaserStatus.Loaded]: each is [TeaserStatus.Error]
     * or [TeaserStatus.Empty]) and at least one of them failed. A partial failure
     * returns null, so per-tile independent degradation is unchanged; so does an
     * all-[TeaserStatus.Empty] dashboard without any error, and a blank teaser list.
     *
     * When the causes differ, the first error in teaser order is returned; that is
     * enough because being offline yields [AppError.NoNetwork] for every teaser.
     */
    val unavailableError: AppError?
        get() = if (
            teasers.isNotEmpty() &&
            teasers.all { it.status == TeaserStatus.Error || it.status == TeaserStatus.Empty }
        ) {
            teasers.firstNotNullOfOrNull { it.error }
        } else {
            null
        }
}
