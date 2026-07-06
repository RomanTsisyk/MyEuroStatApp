package eu.eurostat.core.charts.model

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Value-semantics and default-behavior tests for the chart model types
 * [ChartPoint], [ChartSeries] and [ChartAxis].
 */
class ChartModelTest {

    @Test
    fun chart_point_holds_x_and_y() {
        val p = ChartPoint(x = 2020.0, y = 100.5)
        assertEquals(2020.0, p.x)
        assertEquals(100.5, p.y)
    }

    @Test
    fun chart_point_supports_null_y_for_gaps() {
        assertNull(ChartPoint(x = 2020.0, y = null).y)
    }

    @Test
    fun chart_point_has_value_equality() {
        assertEquals(ChartPoint(1.0, 2.0), ChartPoint(1.0, 2.0))
        assertTrue(ChartPoint(1.0, 2.0) != ChartPoint(1.0, 3.0))
    }

    @Test
    fun chart_series_preserves_label_color_and_point_order() {
        val points = listOf(ChartPoint(1.0, 10.0), ChartPoint(2.0, 20.0), ChartPoint(3.0, null))
        val series = ChartSeries(label = "Germany", color = Color.Red, points = points)
        assertEquals("Germany", series.label)
        assertEquals(Color.Red, series.color)
        assertEquals(points, series.points)
        assertEquals(3, series.points.size)
    }

    @Test
    fun chart_axis_defaults_range_to_null_and_uses_compact_formatter() {
        val axis = ChartAxis(label = "Population")
        assertNull(axis.range)
        // Default formatter delegates to formatCompactNumber (device locale), so
        // assert on the locale-independent magnitude suffix rather than exact text.
        assertTrue(axis.formatter(850_000f).endsWith("K"), "expected K suffix: ${axis.formatter(850_000f)}")
        assertTrue(axis.formatter(1_234_567f).endsWith("M"), "expected M suffix: ${axis.formatter(1_234_567f)}")
    }

    @Test
    fun chart_axis_accepts_custom_formatter_and_range() {
        val axis = ChartAxis(label = "Year", range = 2010f..2020f, formatter = { "${it.toInt()}yr" })
        assertEquals(2010f..2020f, axis.range)
        assertEquals("2015yr", axis.formatter(2015f))
    }
}
