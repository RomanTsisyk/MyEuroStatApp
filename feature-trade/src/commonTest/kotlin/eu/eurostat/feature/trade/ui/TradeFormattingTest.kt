package eu.eurostat.feature.trade.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * Tests for the billions formatters in `TradeScreen.kt`. Inputs are millions
 * of EUR (the unit Eurostat reports); outputs are whole billions rounded to
 * nearest (ties away from zero) so Trade agrees with the Overview screen.
 */
class TradeFormattingTest {

    /** Typographic minus (U+2212), as used by `formatSignedDecimal` on other screens. */
    private val minus = "−"

    @Test
    fun formatSignedBillions_roundsToNearestNotTruncates() {
        // 839.6 B truncated to 839 before; nearest is 840 (Overview shows 840).
        assertEquals("+840", formatSignedBillions(839_600L))
        assertEquals("+89", formatSignedBillions(89_499L))
        assertEquals("+90", formatSignedBillions(89_500L))
    }

    @Test
    fun formatSignedBillions_negativeUsesTypographicMinus() {
        assertEquals("${minus}14", formatSignedBillions(-13_600L))
        assertEquals("${minus}13", formatSignedBillions(-13_400L))
        assertFalse(formatSignedBillions(-13_600L).contains('-'), "ASCII hyphen must not be used")
    }

    @Test
    fun formatSignedBillions_tiesRoundAwayFromZero() {
        assertEquals("+15", formatSignedBillions(14_500L))
        assertEquals("${minus}15", formatSignedBillions(-14_500L))
    }

    @Test
    fun formatSignedBillions_roundsToZeroIsNeverNegativeZero() {
        assertEquals("+0", formatSignedBillions(0L))
        assertEquals("+0", formatSignedBillions(-400L))
        assertEquals("+0", formatSignedBillions(499L))
    }

    @Test
    fun formatSignedBillions_smallNegativeRoundingUpToOne() {
        assertEquals("${minus}1", formatSignedBillions(-999L))
    }

    @Test
    fun formatBillions_roundsToNearestNotTruncates() {
        assertEquals("840", formatBillions(839_600L))
        assertEquals("839", formatBillions(839_499L))
        assertEquals("853", formatBillions(852_500L))
    }

    @Test
    fun formatBillions_negativeRendersAbsoluteMagnitude() {
        assertEquals("854", formatBillions(-853_600L))
        assertEquals("1", formatBillions(-500L))
        assertEquals("0", formatBillions(-499L))
    }

    @Test
    fun headline_exportsTabShowsUnsignedExports() {
        assertEquals("840", tradeHeadlineValue(839_600L, 853_900L, -14_300L, tabIndex = 0))
    }

    @Test
    fun headline_importsTabShowsUnsignedImports() {
        assertEquals("854", tradeHeadlineValue(839_600L, 853_900L, -14_300L, tabIndex = 1))
    }

    @Test
    fun headline_balanceTabShowsSignedBalance() {
        assertEquals("\u221214", tradeHeadlineValue(839_600L, 853_900L, -14_300L, tabIndex = 2))
    }

    @Test
    fun headline_missingSelectedFigureIsDash() {
        assertEquals("\u2014", tradeHeadlineValue(null, 853_900L, -14_300L, tabIndex = 0))
        assertEquals("\u2014", tradeHeadlineValue(839_600L, null, -14_300L, tabIndex = 1))
        assertEquals("\u2014", tradeHeadlineValue(839_600L, 853_900L, null, tabIndex = 2))
    }
}
