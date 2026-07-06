package eu.eurostat.core.common

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests the multiplatform number formatters in [NumberFormatting].
 *
 * Every case pins an explicit BCP-47 `locale` so results are independent of the
 * host/device default locale (CI machines are not guaranteed to be en-US). The
 * android and desktop `actual`s both delegate to `java.text.NumberFormat`, so the
 * expectations hold on both the `testDebugUnitTest` and `desktopTest` targets.
 */
class NumberFormattingTest {

    // ---- formatCompactNumber -------------------------------------------------

    @Test
    fun compact_millions_gets_M_suffix_with_one_decimal() {
        assertEquals("1.2M", formatCompactNumber(1_234_567.0, "en"))
    }

    @Test
    fun compact_thousands_gets_K_suffix() {
        assertEquals("850K", formatCompactNumber(850_000.0, "en"))
    }

    @Test
    fun compact_billions_gets_B_suffix() {
        assertEquals("1.5B", formatCompactNumber(1_500_000_000.0, "en"))
    }

    @Test
    fun compact_below_thousand_is_left_plain() {
        assertEquals("12.3", formatCompactNumber(12.3, "en"))
        assertEquals("999", formatCompactNumber(999.0, "en"))
        assertEquals("0", formatCompactNumber(0.0, "en"))
    }

    @Test
    fun compact_exact_magnitude_boundaries() {
        assertEquals("1K", formatCompactNumber(1_000.0, "en"))
        assertEquals("1M", formatCompactNumber(1_000_000.0, "en"))
        assertEquals("1B", formatCompactNumber(1_000_000_000.0, "en"))
    }

    @Test
    fun compact_negative_keeps_sign() {
        assertEquals("-2M", formatCompactNumber(-2_000_000.0, "en"))
    }

    @Test
    fun compact_uses_locale_decimal_separator() {
        // German uses a comma as the decimal separator.
        assertEquals("1,2M", formatCompactNumber(1_200_000.0, "de"))
    }

    // ---- formatPercent -------------------------------------------------------

    @Test
    fun percent_appends_percent_sign_with_default_one_decimal() {
        assertEquals("12.5%", formatPercent(12.5, 1, "en"))
        assertEquals("15.0%", formatPercent(15.0, 1, "en"))
    }

    @Test
    fun percent_zero_fraction_digits_drops_decimals() {
        assertEquals("15%", formatPercent(15.0, 0, "en"))
    }

    @Test
    fun percent_negative_keeps_sign() {
        assertEquals("-2.5%", formatPercent(-2.5, 1, "en"))
    }

    @Test
    fun percent_uses_locale_decimal_separator() {
        assertEquals("12,5%", formatPercent(12.5, 1, "de"))
    }

    // ---- formatGrouped -------------------------------------------------------

    @Test
    fun grouped_inserts_thousand_separators_en() {
        assertEquals("1,234,567", formatGrouped(1_234_567.0, "en"))
    }

    @Test
    fun grouped_inserts_thousand_separators_de() {
        assertEquals("1.234.567", formatGrouped(1_234_567.0, "de"))
    }

    @Test
    fun grouped_below_thousand_has_no_separator() {
        assertEquals("999", formatGrouped(999.0, "en"))
        assertEquals("0", formatGrouped(0.0, "en"))
    }
}
