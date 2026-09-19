package eu.eurostat.core.charts.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Tests for the [yearAxis] factory. */
class YearAxisTest {

    @Test
    fun year_axis_renders_full_year_not_compact_suffix() {
        val axis = yearAxis("year")
        assertEquals("2010", axis.formatter(2010f))
        assertEquals("2024", axis.formatter(2024f))
    }

    @Test
    fun default_axis_still_uses_compact_formatting() {
        // Guards the reason yearAxis exists: the default collapses years.
        assertEquals("2K", ChartAxis(label = "x").formatter(2010f))
    }

    @Test
    fun year_axis_keeps_label_and_auto_range() {
        val axis = yearAxis("Year")
        assertEquals("Year", axis.label)
        assertNull(axis.range)
    }

    @Test
    fun year_axis_rounds_fractional_tick_to_nearest_year() {
        assertEquals("2011", yearAxis("").formatter(2010.6f))
    }
}
