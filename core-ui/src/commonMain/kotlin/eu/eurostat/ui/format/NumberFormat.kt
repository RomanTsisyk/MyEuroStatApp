package eu.eurostat.ui.format

import kotlin.math.abs
import kotlin.math.round

/** Unicode minus sign (U+2212) used for negative values, matching editorial typography. */
private const val MINUS_SIGN = '−'

/** Compact magnitude suffixes applied by [formatLargeNumber] / [formatLargeNumberParts]. */
private const val SUFFIX_BILLION = "B"
private const val SUFFIX_MILLION = "M"
private const val SUFFIX_THOUSAND = "K"

private const val ONE_THOUSAND = 1_000L
private const val ONE_MILLION = 1_000_000L
private const val ONE_BILLION = 1_000_000_000L

/**
 * Locale-aware number formatting shared by every feature screen.
 *
 * Every feature module used to carry its own private `formatXxx` helpers with
 * subtly different rounding and a hardcoded `.` decimal / `,` grouping
 * separator. This file is the single source of truth for that arithmetic;
 * screens should only keep thin, screen-local wrappers that add a domain
 * suffix (e.g. `"€"`, `"bn"`) around these calls.
 *
 * Locale awareness comes from [localeDecimalSeparator] / [localeGroupingSeparator],
 * which are `expect`/`actual` per platform (Android + desktop via
 * `java.text.DecimalFormatSymbols`, iOS via `NSNumberFormatter`/`NSLocale`).
 */

/**
 * Rounds [value] to [decimals] fractional digits and renders it using the
 * platform locale's decimal separator, e.g. `12.34` -> `"12.3"` (en) or
 * `"12,3"` (de) for `decimals = 1`.
 *
 * Uses half-up rounding on the absolute value. A value that rounds to
 * exactly zero renders as `"0"` (never `"-0"`), but a value that rounds to a
 * genuine non-zero negative whose whole part truncates to zero — e.g. `-0.4`
 * at 1 decimal — keeps its leading `-` (e.g. `"-0.4"`, never a sign-losing
 * `"0.4"`).
 */
fun formatDecimal(value: Double, decimals: Int = 1): String {
    val rounded = roundTo(value, decimals)
    if (rounded == 0.0) return if (decimals <= 0) "0" else "0${localeDecimalSeparator()}${"0".repeat(decimals)}"
    val whole = rounded.toLong()
    if (decimals <= 0) return whole.toString()
    val sep = localeDecimalSeparator()
    val factor = pow10(decimals)
    val fracLong = (round(abs(rounded - whole) * factor)).toLong()
    val fracStr = fracLong.toString().padStart(decimals, '0')
    // whole truncates toward zero, so a value like -0.4 gives whole == 0 and
    // would otherwise render as "0.4", silently dropping the sign.
    val sign = if (rounded < 0.0 && whole == 0L) "-" else ""
    return "$sign$whole$sep$fracStr"
}

/**
 * Same rounding as [formatDecimal] but always renders a leading sign: `+` for
 * values `>= 0`, or the Unicode minus sign (U+2212) for negatives — matching
 * the editorial typography used across the economy/environment screens.
 *
 * A value that rounds to exactly zero at the requested precision is treated
 * as non-negative and rendered with a leading `+`.
 */
fun formatSignedDecimal(value: Double, decimals: Int = 1): String {
    val formatted = formatDecimal(abs(value), decimals)
    val isZero = roundTo(value, decimals) == 0.0
    return if (value < 0.0 && !isZero) "$MINUS_SIGN$formatted" else "+$formatted"
}

/** [formatDecimal] with a trailing `%`, e.g. `formatPercent(12.5)` -> `"12.5%"`. */
fun formatPercent(value: Double, decimals: Int = 1): String = "${formatDecimal(value, decimals)}%"

/** [formatSignedDecimal] with a trailing `%`, e.g. `"+6.2%"`, `"−0.4%"`. */
fun formatSignedPercent(value: Double, decimals: Int = 1): String =
    "${formatSignedDecimal(value, decimals)}%"

/**
 * Splits [value] into a compact (magnitude, suffix) pair for headline display,
 * e.g. `83_200_000L` -> `"83.2" to "M"`, `1_500_000_000L` -> `"1.5" to "B"`,
 * `689_000L` -> `"689.0" to "K"`, `873L` -> `"873" to ""`.
 *
 * The bucket is chosen on the *rounded* magnitude, so a value whose rounding
 * carries across a unit boundary promotes to the next unit — `999_972L` at
 * one decimal is `"1.0" to "M"`, never `"1000.0" to "K"`. (The B bucket has
 * no higher unit to promote into.)
 *
 * This is a pure magnitude bucketer: negative and zero values are formatted
 * like any other (e.g. `0L` -> `"0" to ""`). Screens that treat a
 * non-positive value as "no data" (e.g. a population total that can never
 * legitimately be zero) should check that themselves before calling in —
 * see [formatLargeNumber] for that convention.
 */
fun formatLargeNumberParts(value: Long, decimals: Int = 1): Pair<String, String> {
    val absValue = abs(value)
    if (absValue < ONE_THOUSAND) return value.toString() to ""
    val rolledToBillions = abs(roundTo(value / ONE_MILLION.toDouble(), decimals)) >= ONE_THOUSAND.toDouble()
    val rolledToMillions = abs(roundTo(value / ONE_THOUSAND.toDouble(), decimals)) >= ONE_THOUSAND.toDouble()
    return when {
        absValue >= ONE_BILLION || rolledToBillions ->
            formatDecimal(value / ONE_BILLION.toDouble(), decimals) to SUFFIX_BILLION
        absValue >= ONE_MILLION || rolledToMillions ->
            formatDecimal(value / ONE_MILLION.toDouble(), decimals) to SUFFIX_MILLION
        else ->
            formatDecimal(value / ONE_THOUSAND.toDouble(), decimals) to SUFFIX_THOUSAND
    }
}

/**
 * Compact human-readable count combining [formatLargeNumberParts] into a
 * single string, e.g. `40_900_000L` -> `"40.9 M"`, `1_500_000_000L` -> `"1.5 B"`,
 * `873L` -> `"873"`. Returns `"—"` for values `<= 0`, matching the
 * population/legend headlines that treat a non-positive total as missing data.
 */
fun formatLargeNumber(value: Long, decimals: Int = 1): String {
    if (value <= 0L) return "—"
    val (magnitude, suffix) = formatLargeNumberParts(value, decimals)
    return if (suffix.isEmpty()) magnitude else "$magnitude $suffix"
}

/**
 * Converts a value expressed in millions of EUR (as Eurostat datasets report
 * GDP-style figures) into a billions-of-EUR decimal string, e.g.
 * `formatBillionsFromMillions(3_451_000L)` -> `"3451.0"` for `decimals = 1`,
 * or with `decimals = 0` -> `"3451"`.
 *
 * This performs only the unit conversion + rounding; screens that also need
 * thousands grouping on the result should pipe it through [formatGrouped]
 * themselves (grouping needs a [Long] to avoid ambiguity around the decimal
 * point).
 */
fun formatBillionsFromMillions(valueMEur: Long, decimals: Int = 1): String =
    formatDecimal(valueMEur / ONE_THOUSAND.toDouble(), decimals)

/**
 * Formats [value] with locale-aware thousands grouping and no fractional
 * digits, e.g. `3451L` -> `"3 451"` when the locale groups with a thin space,
 * or `"3,451"` for a comma-grouped locale. Negative values keep a leading
 * ASCII `-` (grouping is a magnitude-only concern; callers needing the
 * editorial `+`/`−` sign convention should use [formatSignedDecimal] instead).
 */
fun formatGrouped(value: Long): String {
    val sep = localeGroupingSeparator()
    val raw = abs(value).toString()
    val parts = mutableListOf<String>()
    var i = raw.length
    while (i > 0) {
        val start = maxOf(0, i - GROUP_SIZE)
        parts.add(0, raw.substring(start, i))
        i = start
    }
    val joined = parts.joinToString(sep.toString())
    return if (value < 0L) "-$joined" else joined
}

private const val GROUP_SIZE = 3

/**
 * Rounds [value] to [decimals] fractional digits, ties rounding away from
 * zero (`12.5` -> `13`, `-12.5` -> `-13`). [kotlin.math.round] itself uses
 * ties-to-even (banker's rounding), which is not what any of the screens
 * this API replaces expect, so ties are broken manually here.
 */
private fun roundTo(value: Double, decimals: Int): Double {
    val factor = pow10(decimals)
    val scaled = value * factor
    val roundedAwayFromZero = if (scaled >= 0.0) {
        kotlin.math.floor(scaled + 0.5)
    } else {
        kotlin.math.ceil(scaled - 0.5)
    }
    return roundedAwayFromZero / factor
}

private fun pow10(n: Int): Double {
    var r = 1.0
    repeat(n) { r *= 10.0 }
    return r
}

/**
 * The decimal point/comma the host platform's default locale uses to
 * separate the integer and fractional parts of a number.
 *
 * Android/desktop: `java.text.DecimalFormatSymbols.getInstance()`.
 * iOS: `NSNumberFormatter` seeded with `NSLocale.currentLocale`.
 */
internal expect fun localeDecimalSeparator(): Char

/**
 * The digit-group separator the host platform's default locale uses for
 * thousands grouping (e.g. `,` in `en-US`, `.` in `de-DE`, a thin space in
 * `fr-FR`).
 */
internal expect fun localeGroupingSeparator(): Char
