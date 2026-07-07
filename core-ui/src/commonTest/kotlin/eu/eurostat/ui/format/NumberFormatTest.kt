package eu.eurostat.ui.format

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests the shared formatters in `NumberFormat.kt`.
 *
 * The shared API has no locale parameter — it always reads the platform's
 * default locale via [localeDecimalSeparator] / [localeGroupingSeparator] — so
 * these tests build their expectations from those same functions rather than
 * hardcoding `.` / `,`. That keeps the suite green regardless of the host or
 * CI machine's default locale (verified: this repo's JVM test environment
 * resolves to `uk_PL`, whose decimal separator is `,` and grouping is a
 * non-breaking space, not `en-US` conventions) while still fully exercising
 * the rounding, sign, and magnitude-bucketing logic.
 */
class NumberFormatTest {

    private val dec = localeDecimalSeparator()
    private val grp = localeGroupingSeparator()

    // ---- formatDecimal --------------------------------------------------

    @Test
    fun decimal_rounds_to_requested_precision() {
        assertEquals("12${dec}3", formatDecimal(12.34, 1))
        assertEquals("12${dec}35", formatDecimal(12.345, 2))
    }

    @Test
    fun decimal_zero_digits_drops_fraction() {
        assertEquals("12", formatDecimal(12.49, 0))
        assertEquals("13", formatDecimal(12.5, 0))
    }

    @Test
    fun decimal_zero_value() {
        assertEquals("0${dec}0", formatDecimal(0.0, 1))
    }

    @Test
    fun decimal_negative_rounding_never_yields_negative_zero() {
        assertEquals("0", formatDecimal(-0.049, 0))
    }

    @Test
    fun decimal_pads_fraction_with_trailing_zero() {
        assertEquals("15${dec}0", formatDecimal(15.0, 1))
    }

    @Test
    fun decimal_negative_between_minus_one_and_zero_keeps_sign() {
        // whole part truncates to 0 here; naively concatenating "$whole$sep$frac"
        // would silently drop the negative sign and render "0.4" instead of "-0.4".
        assertEquals("-0${dec}4", formatDecimal(-0.4, 1))
    }

    // ---- formatSignedDecimal ---------------------------------------------

    @Test
    fun signed_decimal_positive_gets_plus() {
        assertEquals("+6${dec}2", formatSignedDecimal(6.2, 1))
    }

    @Test
    fun signed_decimal_negative_gets_unicode_minus() {
        assertEquals("−0${dec}4", formatSignedDecimal(-0.4, 1))
    }

    @Test
    fun signed_decimal_zero_gets_plus() {
        assertEquals("+0${dec}0", formatSignedDecimal(0.0, 1))
    }

    @Test
    fun signed_decimal_rounds_to_zero_still_gets_plus() {
        // Rounds to 0.0 at 1 decimal, so must not render a minus sign.
        assertEquals("+0${dec}0", formatSignedDecimal(-0.04, 1))
    }

    // ---- formatPercent / formatSignedPercent ------------------------------

    @Test
    fun percent_appends_percent_sign() {
        assertEquals("12${dec}5%", formatPercent(12.5, 1))
        assertEquals("15${dec}0%", formatPercent(15.0, 1))
    }

    @Test
    fun signed_percent_matches_signed_decimal_shape() {
        assertEquals("+6${dec}2%", formatSignedPercent(6.2, 1))
        assertEquals("−0${dec}4%", formatSignedPercent(-0.4, 1))
        assertEquals("+0${dec}0%", formatSignedPercent(0.0, 1))
    }

    // ---- formatLargeNumberParts / formatLargeNumber -----------------------

    @Test
    fun large_number_billions_bucket() {
        assertEquals("1${dec}5" to "B", formatLargeNumberParts(1_500_000_000L))
        assertEquals("1${dec}5 B", formatLargeNumber(1_500_000_000L))
    }

    @Test
    fun large_number_millions_bucket() {
        assertEquals("83${dec}2" to "M", formatLargeNumberParts(83_200_000L))
        assertEquals("83${dec}2 M", formatLargeNumber(83_200_000L))
    }

    @Test
    fun large_number_thousands_bucket() {
        // Matches the original Population/Tourism screens: the K/M/B buckets
        // always show one decimal at the default precision, including for
        // round thousands (e.g. "689.0 K", not "689 K").
        assertEquals("689${dec}0" to "K", formatLargeNumberParts(689_000L))
        assertEquals("689${dec}0 K", formatLargeNumber(689_000L))
    }

    @Test
    fun large_number_below_thousand_is_plain() {
        assertEquals("873" to "", formatLargeNumberParts(873L))
        assertEquals("873", formatLargeNumber(873L))
    }

    @Test
    fun large_number_exact_magnitude_boundaries() {
        assertEquals("1${dec}0" to "K", formatLargeNumberParts(1_000L))
        assertEquals("1${dec}0" to "M", formatLargeNumberParts(1_000_000L))
        assertEquals("1${dec}0" to "B", formatLargeNumberParts(1_000_000_000L))
    }

    @Test
    fun large_number_rounding_rollover_promotes_to_next_bucket() {
        // Rounding can carry across a unit boundary; the bucket must be chosen
        // on the rounded magnitude so these promote instead of rendering a
        // four-digit magnitude like "1000.0 K" / "1000.0 M".
        assertEquals("1${dec}0" to "M", formatLargeNumberParts(999_972L))
        assertEquals("1${dec}0 M", formatLargeNumber(999_972L))
        assertEquals("1${dec}0" to "B", formatLargeNumberParts(999_950_000L))
        assertEquals("-1${dec}0" to "M", formatLargeNumberParts(-999_972L))
    }

    @Test
    fun large_number_just_below_rollover_stays_in_bucket() {
        assertEquals("999${dec}9" to "K", formatLargeNumberParts(999_949L))
        assertEquals("999${dec}9" to "M", formatLargeNumberParts(999_949_999L))
    }

    @Test
    fun large_number_zero_and_negative_render_em_dash() {
        // formatLargeNumber() applies the "non-positive total = no data" policy...
        assertEquals("—", formatLargeNumber(0L))
        assertEquals("—", formatLargeNumber(-5_000_000L))
    }

    @Test
    fun large_number_parts_is_a_pure_bucketer_with_no_dash_policy() {
        // ...but the lower-level formatLargeNumberParts() does not: it formats
        // zero/negative like any other magnitude, since screens like Tourism
        // treat a real 0-nights data point as legitimate, not "missing".
        assertEquals("0" to "", formatLargeNumberParts(0L))
        assertEquals("-5${dec}0" to "M", formatLargeNumberParts(-5_000_000L))
    }

    @Test
    fun large_number_long_max_boundary_sanity() {
        // Long.MAX_VALUE is ~9.22e18, i.e. ~9223372036.9 billion — must not
        // throw or overflow when divided down into the billions bucket.
        val (value, unit) = formatLargeNumberParts(Long.MAX_VALUE)
        assertEquals("B", unit)
        assertEquals("9223372036${dec}9", value)
    }

    // ---- formatBillionsFromMillions ---------------------------------------

    @Test
    fun billions_from_millions_converts_and_rounds() {
        assertEquals("3451${dec}0", formatBillionsFromMillions(3_451_000L, 1))
        assertEquals("3451", formatBillionsFromMillions(3_451_000L, 0))
    }

    @Test
    fun billions_from_millions_zero() {
        assertEquals("0${dec}0", formatBillionsFromMillions(0L, 1))
    }

    @Test
    fun billions_from_millions_negative() {
        assertEquals("-1${dec}0", formatBillionsFromMillions(-1_000L, 1))
    }

    // ---- formatGrouped ------------------------------------------------------

    @Test
    fun grouped_inserts_thousands_separators() {
        assertEquals("3${grp}451", formatGrouped(3_451L))
        assertEquals("1${grp}234${grp}567", formatGrouped(1_234_567L))
    }

    @Test
    fun grouped_below_thousand_has_no_separator() {
        assertEquals("999", formatGrouped(999L))
        assertEquals("0", formatGrouped(0L))
    }

    @Test
    fun grouped_negative_keeps_ascii_minus() {
        assertEquals("-3${grp}451", formatGrouped(-3_451L))
    }

    @Test
    fun grouped_long_max_boundary_sanity() {
        assertEquals("9${grp}223${grp}372${grp}036${grp}854${grp}775${grp}807", formatGrouped(Long.MAX_VALUE))
    }
}
