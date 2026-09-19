package eu.eurostat.feature.transport.ui

import eu.eurostat.ui.format.formatDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for the StatTile value formatters in `TransportScreen.kt`. The
 * magnitude and unit must be joined by a no-break space (U+00A0) so a narrow
 * tile can never wrap "200 M" onto two lines.
 */
class TransportFormattingTest {

    private val nbsp = " "

    /** Platform decimal separator, derived through the same formatter under test. */
    private val dec = formatDecimal(1.5, 1).substring(1, 2)

    @Test
    fun formatMillions_underHundred_keepsOneDecimalAndNoBreakSpace() {
        assertEquals("57${dec}8${nbsp}M", formatMillions(57_800_000L, "M"))
    }

    @Test
    fun formatMillions_hundredOrMore_dropsDecimalAndUsesNoBreakSpace() {
        assertEquals("200${nbsp}M", formatMillions(200_000_000L, "M"))
        assertEquals("185${nbsp}M", formatMillions(185_300_000L, "M"))
    }

    @Test
    fun formatBillions_usesNoBreakSpace() {
        assertEquals("5${dec}2${nbsp}bn", formatBillions(5_200_000_000L, "bn"))
    }

    @Test
    fun formatters_neverEmitOrdinarySpace_evenWithLongLocalizedUnits() {
        listOf(
            formatMillions(200_000_000L, "млн"),
            formatMillions(12_300_000L, "mln"),
            formatBillions(4_100_000_000L, "млрд"),
        ).forEach { formatted ->
            assertFalse(formatted.contains(' '), "ordinary space would allow a wrap: '$formatted'")
            assertTrue(formatted.contains(' '), "expected a no-break space in '$formatted'")
        }
    }
}
