package eu.eurostat.feature.overview.ui

import eu.eurostat.core.common.formatCompactNumber
import eu.eurostat.core.common.formatGrouped
import eu.eurostat.core.common.formatPercent

/** Text shown in place of a headline value that is not (yet) available. */
internal const val NO_VALUE = "—"

private const val MILLIONS_PER_BILLION = 1_000.0
private const val PERCENT_FRACTION_DIGITS = 1

/**
 * Renders a raw teaser [value] as display text according to [format], using
 * the locale currently in effect.
 *
 * This is intentionally a plain function evaluated during composition rather
 * than something the component precomputes: separators and rounding follow
 * the *current* locale, so the output changes when the user switches language
 * at runtime while the raw state stays the same.
 *
 * Rounding is unchanged from the previous string-based state: GDP/trade
 * amounts (EUR millions) are converted to billions and rounded to the nearest
 * whole billion by the grouped formatter.
 *
 * @param locale BCP-47 tag to format for, or null for the platform's current
 *   default locale (what production code uses; tests pass an explicit tag).
 */
internal fun formatTeaserValue(value: Double, format: TeaserFormat, locale: String? = null): String =
    when (format) {
        TeaserFormat.Compact -> formatCompactNumber(value, locale)
        TeaserFormat.BillionsFromMillions -> formatGrouped(value / MILLIONS_PER_BILLION, locale)
        TeaserFormat.Grouped -> formatGrouped(value, locale)
        TeaserFormat.Percent -> formatPercent(value, PERCENT_FRACTION_DIGITS, locale)
    }

/**
 * The teaser's headline as display text: the formatted [ModuleTeaser.value],
 * or [NO_VALUE] when the teaser has none. Call from composition (see
 * [formatTeaserValue]).
 */
internal fun ModuleTeaser.displayValue(locale: String? = null): String =
    value?.let { formatTeaserValue(it, format, locale) } ?: NO_VALUE
