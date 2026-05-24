package eu.eurostat.core.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import eu.eurostat.core.charts.internal.ChartDefaults

/**
 * One age cohort row of a [EurostatPyramidChart].
 *
 * @property label Cohort label (e.g. "60-64").
 * @property male Magnitude of the male side (drawn on the left).
 * @property female Magnitude of the female side (drawn on the right).
 */
data class PyramidRow(
    val label: String,
    val male: Float,
    val female: Float,
)

/**
 * Demographic age pyramid. Mirrors at the vertical center: males extend left, females right.
 *
 * @param cohorts Rows from oldest (top) to youngest (bottom).
 * @param maleColor Bar color for the male side.
 * @param femaleColor Bar color for the female side.
 * @param activeIndex Optional emphasized cohort; other cohorts dim to muted.
 * @param modifier Layout modifier.
 */
@Composable
fun EurostatPyramidChart(
    cohorts: List<PyramidRow>,
    maleColor: Color = ChartDefaults.Accent,
    femaleColor: Color = ChartDefaults.AccentSecondary,
    activeIndex: Int? = null,
    modifier: Modifier = Modifier,
) {
    if (cohorts.isEmpty()) {
        Box(modifier = modifier)
        return
    }
    val maxSide = cohorts.maxOf { maxOf(it.male, it.female) }.coerceAtLeast(0.0001f)
    val muted = ChartDefaults.MutedAlt
    Canvas(modifier = modifier) {
        val n = cohorts.size
        val rowH = size.height / n
        val gap = rowH * 0.25f
        val barH = rowH - gap
        val midX = size.width / 2f
        cohorts.forEachIndexed { i, row ->
            val y = i * rowH + gap / 2f
            val mw = row.male / maxSide * (midX - 1f)
            val fw = row.female / maxSide * (midX - 1f)
            val mc = if (activeIndex == null || activeIndex == i) maleColor else muted
            val fc = if (activeIndex == null || activeIndex == i) femaleColor else muted
            drawRect(
                color = mc,
                topLeft = Offset(midX - mw, y),
                size = Size(mw, barH),
            )
            drawRect(
                color = fc,
                topLeft = Offset(midX, y),
                size = Size(fw, barH),
            )
        }
    }
}

/** Preview composable for [EurostatPyramidChart]. */
@Composable
fun EurostatPyramidChartPreview() {
    val cohorts = listOf(
        PyramidRow("80+", 1.2f, 1.8f),
        PyramidRow("60-79", 4.4f, 4.8f),
        PyramidRow("40-59", 6.6f, 6.5f),
        PyramidRow("20-39", 5.9f, 5.7f),
        PyramidRow("0-19", 4.0f, 3.8f),
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.4f)
            .background(ChartDefaults.Paper)
    ) {
        EurostatPyramidChart(cohorts = cohorts, modifier = Modifier.fillMaxSize())
    }
}
