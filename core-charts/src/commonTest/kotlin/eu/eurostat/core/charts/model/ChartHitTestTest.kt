package eu.eurostat.core.charts.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

/**
 * Tests for [nearestChartPoint] — the pure tap → data-point hit-test behind the
 * "detail modal on chart tap" feature. All cases use an identity projection
 * (`x`/`y` map straight to pixels) so the geometry stays easy to reason about.
 */
class ChartHitTestTest {

    // Identity projection: chart value == pixel coordinate.
    private val identityX: (Double) -> Float = { it.toFloat() }
    private val identityY: (Double) -> Float = { it.toFloat() }

    private fun series(label: String, vararg points: Pair<Double, Double?>): ChartSeries =
        ChartSeries(
            label = label,
            color = Color.Red,
            points = points.map { (x, y) -> ChartPoint(x, y) },
        )

    @Test
    fun returns_null_for_empty_series_list() {
        val hit = nearestChartPoint(
            series = emptyList(),
            tap = Offset(10f, 10f),
            mapX = identityX,
            mapY = identityY,
            thresholdPx = 24f,
        )
        assertNull(hit)
    }

    @Test
    fun returns_null_when_all_series_are_empty() {
        val hit = nearestChartPoint(
            series = listOf(series("A"), series("B")),
            tap = Offset(0f, 0f),
            mapX = identityX,
            mapY = identityY,
            thresholdPx = 24f,
        )
        assertNull(hit)
    }

    @Test
    fun picks_the_nearest_point_within_threshold() {
        val s = series("A", 0.0 to 0.0, 10.0 to 10.0, 100.0 to 100.0)
        val hit = nearestChartPoint(
            series = listOf(s),
            tap = Offset(12f, 9f), // closest to (10,10)
            mapX = identityX,
            mapY = identityY,
            thresholdPx = 24f,
        )
        assertEquals(ChartPoint(10.0, 10.0), hit?.second)
        assertSame(s, hit?.first)
    }

    @Test
    fun picks_the_nearest_across_multiple_series() {
        val a = series("A", 0.0 to 0.0)
        val b = series("B", 50.0 to 50.0)
        val hit = nearestChartPoint(
            series = listOf(a, b),
            tap = Offset(48f, 51f), // closest to B's (50,50)
            mapX = identityX,
            mapY = identityY,
            thresholdPx = 24f,
        )
        assertEquals("B", hit?.first?.label)
        assertEquals(ChartPoint(50.0, 50.0), hit?.second)
    }

    @Test
    fun threshold_excludes_a_far_tap() {
        val s = series("A", 0.0 to 0.0, 100.0 to 100.0)
        val hit = nearestChartPoint(
            series = listOf(s),
            tap = Offset(50f, 50f), // ~70px from either point, threshold 24
            mapX = identityX,
            mapY = identityY,
            thresholdPx = 24f,
        )
        assertNull(hit)
    }

    @Test
    fun includes_a_point_exactly_at_the_threshold_distance() {
        val s = series("A", 0.0 to 0.0)
        val hit = nearestChartPoint(
            series = listOf(s),
            tap = Offset(24f, 0f), // exactly 24px away
            mapX = identityX,
            mapY = identityY,
            thresholdPx = 24f,
        )
        assertEquals(ChartPoint(0.0, 0.0), hit?.second)
    }

    @Test
    fun skips_null_y_gaps_and_hits_the_next_real_point() {
        // Nearest *drawn* point to the tap is (10,10); the closer (12, null) is a gap.
        val s = series("A", 10.0 to 10.0, 12.0 to null, 80.0 to 80.0)
        val hit = nearestChartPoint(
            series = listOf(s),
            tap = Offset(12f, 12f),
            mapX = identityX,
            mapY = identityY,
            thresholdPx = 24f,
        )
        assertEquals(ChartPoint(10.0, 10.0), hit?.second)
    }

    @Test
    fun returns_null_when_the_only_series_is_all_gaps() {
        val s = series("A", 1.0 to null, 2.0 to null)
        val hit = nearestChartPoint(
            series = listOf(s),
            tap = Offset(1f, 0f),
            mapX = identityX,
            mapY = identityY,
            thresholdPx = 24f,
        )
        assertNull(hit)
    }

    @Test
    fun tie_break_keeps_the_first_encountered_point() {
        // Two points equidistant from the tap; the earlier series must win.
        val a = series("A", 0.0 to 0.0)
        val b = series("B", 20.0 to 0.0)
        val hit = nearestChartPoint(
            series = listOf(a, b),
            tap = Offset(10f, 0f), // 10px from each
            mapX = identityX,
            mapY = identityY,
            thresholdPx = 24f,
        )
        assertEquals("A", hit?.first?.label)
    }

    @Test
    fun honors_a_non_identity_projection() {
        // Year → pixel projection: x = (year - 2000) * 10; y passes through.
        val s = series("DE", 2000.0 to 5.0, 2010.0 to 5.0)
        val mapX: (Double) -> Float = { ((it - 2000.0) * 10.0).toFloat() }
        val hit = nearestChartPoint(
            series = listOf(s),
            tap = Offset(98f, 5f), // near year 2010 → pixel x=100
            mapX = mapX,
            mapY = identityY,
            thresholdPx = 24f,
        )
        assertEquals(2010.0, hit?.second?.x)
    }
}
