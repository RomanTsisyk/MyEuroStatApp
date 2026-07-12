package eu.eurostat.core.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import eu.eurostat.core.charts.internal.ChartDefaults

/**
 * A single category row of an [EurostatDivergingBarChart].
 *
 * @property label Category label.
 * @property exports Positive-direction magnitude (rendered above the zero line).
 * @property imports Negative-direction magnitude (rendered below the zero line).
 */
data class DivergingBarRow(
    val label: String,
    val exports: Float,
    val imports: Float,
)

/**
 * Diverging bar chart. Exports point up from the zero baseline; imports point down.
 *
 * @param data Rows to plot.
 * @param exportColor Fill color for the upward (exports) bar segment.
 * @param importColor Fill color for the downward (imports) bar segment.
 * @param modifier Layout modifier.
 */
@Composable
fun EurostatDivergingBarChart(
    data: List<DivergingBarRow>,
    exportColor: Color = ChartDefaults.Accent,
    importColor: Color = ChartDefaults.AccentSecondary,
    modifier: Modifier = Modifier,
) {
    if (data.isEmpty()) {
        Box(modifier = modifier)
        return
    }
    val maxSide = remember(data) {
        data.maxOf { maxOf(it.exports, it.imports) }.coerceAtLeast(0.0001f)
    }
    Canvas(modifier = modifier) {
        val n = data.size
        val slot = size.width / n
        val gapRatio = 0.32f
        val barW = slot * (1f - gapRatio)
        val midY = size.height / 2f
        drawLine(
            color = ChartDefaults.Ink.copy(alpha = 0.4f),
            start = Offset(0f, midY),
            end = Offset(size.width, midY),
            strokeWidth = ChartDefaults.ThinStrokeDp.toPx(),
        )
        data.forEachIndexed { i, row ->
            val x = i * slot + (slot - barW) / 2f
            val eh = row.exports / maxSide * midY
            val ih = row.imports / maxSide * midY
            drawRect(
                color = exportColor,
                topLeft = Offset(x, midY - eh),
                size = Size(barW, eh),
            )
            drawRect(
                color = importColor,
                topLeft = Offset(x, midY),
                size = Size(barW, ih),
            )
        }
    }
}

/** Preview composable for [EurostatDivergingBarChart]. */
@Composable
fun EurostatDivergingBarChartPreview() {
    val rows = (2018..2023).map { y ->
        val seed = (y - 2018).toFloat()
        DivergingBarRow(y.toString(), 120f + seed * 14f, 90f + seed * 16f)
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(2f)
            .background(ChartDefaults.Paper)
    ) {
        EurostatDivergingBarChart(data = rows, modifier = Modifier.fillMaxSize())
    }
}
