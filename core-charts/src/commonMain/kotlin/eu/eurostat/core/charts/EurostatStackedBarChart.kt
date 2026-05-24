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
 * A single row in an [EurostatStackedBarChart], composed of two stacked segments.
 *
 * @property label Category label for the row.
 * @property bottom Magnitude of the lower segment.
 * @property top Magnitude of the upper segment, stacked on top of [bottom].
 */
data class StackedBarRow(
    val label: String,
    val bottom: Float,
    val top: Float,
)

/**
 * Two-segment stacked bar chart, e.g. domestic vs foreign components per year.
 *
 * @param data Rows to render.
 * @param bottomColor Fill for the lower segment.
 * @param topColor Fill for the upper segment.
 * @param modifier Layout modifier.
 */
@Composable
fun EurostatStackedBarChart(
    data: List<StackedBarRow>,
    bottomColor: Color = ChartDefaults.Accent,
    topColor: Color = ChartDefaults.AccentSecondary,
    modifier: Modifier = Modifier,
) {
    if (data.isEmpty()) {
        Box(modifier = modifier)
        return
    }
    val totals = data.map { it.bottom + it.top }
    val max = totals.max().coerceAtLeast(0.0001f)

    Canvas(modifier = modifier) {
        val n = data.size
        val slot = size.width / n
        val gapRatio = 0.3f
        val barWidth = slot * (1f - gapRatio)
        data.forEachIndexed { i, row ->
            val bottomH = (row.bottom / max) * size.height
            val topH = (row.top / max) * size.height
            val x = i * slot + (slot - barWidth) / 2f
            drawRect(
                color = bottomColor,
                topLeft = Offset(x, size.height - bottomH),
                size = Size(barWidth, bottomH),
            )
            drawRect(
                color = topColor,
                topLeft = Offset(x, size.height - bottomH - topH),
                size = Size(barWidth, topH),
            )
        }
    }
}

/** Preview composable for [EurostatStackedBarChart]. */
@Composable
fun EurostatStackedBarChartPreview() {
    val rows = (2015..2023).mapIndexed { i, y ->
        StackedBarRow(
            label = y.toString(),
            bottom = 100f + i * 6f,
            top = 60f + i * 4f,
        )
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(2f)
            .background(ChartDefaults.Paper)
    ) {
        EurostatStackedBarChart(
            data = rows,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
