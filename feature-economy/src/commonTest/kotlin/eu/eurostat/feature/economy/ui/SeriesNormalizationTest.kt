package eu.eurostat.feature.economy.ui

import androidx.compose.ui.graphics.Color
import eu.eurostat.core.charts.model.ChartPoint
import eu.eurostat.core.charts.model.ChartSeries
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for [rebaseToIndex] — the pure index-rebasing step behind the
 * Economy chart's "Indexed 100" comparison mode.
 */
class SeriesNormalizationTest {

    private fun series(vararg yValues: Double?, label: String = "DE"): ChartSeries =
        ChartSeries(
            label = label,
            color = Color.Red,
            points = yValues.mapIndexed { i, y -> ChartPoint(x = 2010.0 + i, y = y) },
        )

    @Test
    fun rebases_so_first_point_equals_100() {
        val result = rebaseToIndex(listOf(series(50.0, 75.0, 100.0)))

        assertEquals(1, result.size)
        assertEquals(listOf(100.0, 150.0, 200.0), result.single().points.map { it.y })
    }

    @Test
    fun rebases_each_series_against_its_own_first_value() {
        val result = rebaseToIndex(
            listOf(
                series(200.0, 300.0, label = "DE"),
                series(50.0, 75.0, label = "PL"),
            ),
        )

        assertEquals(listOf(100.0, 150.0), result[0].points.map { it.y })
        assertEquals(listOf(100.0, 150.0), result[1].points.map { it.y })
    }

    @Test
    fun drops_series_whose_first_value_is_zero() {
        val result = rebaseToIndex(
            listOf(
                series(0.0, 50.0, label = "XX"),
                // 125/100 is exact in binary, keeping the equality assertion stable.
                series(100.0, 125.0, label = "DE"),
            ),
        )

        assertEquals(listOf("DE"), result.map { it.label })
        assertEquals(listOf(100.0, 125.0), result.single().points.map { it.y })
    }

    @Test
    fun drops_series_whose_first_value_is_a_null_gap() {
        val result = rebaseToIndex(listOf(series(null, 50.0, 60.0)))

        assertTrue(result.isEmpty())
    }

    @Test
    fun drops_empty_series_but_keeps_others() {
        val result = rebaseToIndex(
            listOf(
                series(label = "EMPTY"),
                series(80.0, 88.0, label = "FR"),
            ),
        )

        assertEquals(listOf("FR"), result.map { it.label })
    }

    @Test
    fun single_point_series_becomes_exactly_100() {
        val result = rebaseToIndex(listOf(series(42.0)))

        assertEquals(listOf(100.0), result.single().points.map { it.y })
    }

    @Test
    fun preserves_null_gaps_after_the_first_point() {
        val result = rebaseToIndex(listOf(series(50.0, null, 100.0)))

        assertEquals(listOf(100.0, null, 200.0), result.single().points.map { it.y })
    }

    @Test
    fun preserves_label_color_and_x_values() {
        val result = rebaseToIndex(listOf(series(10.0, 20.0, label = "PL")))

        val s = result.single()
        assertEquals("PL", s.label)
        assertEquals(Color.Red, s.color)
        assertEquals(listOf(2010.0, 2011.0), s.points.map { it.x })
    }

    @Test
    fun empty_input_returns_empty_list() {
        assertTrue(rebaseToIndex(emptyList()).isEmpty())
    }
}
