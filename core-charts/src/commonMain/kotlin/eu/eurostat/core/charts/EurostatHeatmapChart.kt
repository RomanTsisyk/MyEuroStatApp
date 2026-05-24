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
import androidx.compose.ui.graphics.drawscope.Stroke
import eu.eurostat.core.charts.internal.ChartDefaults
import eu.eurostat.core.charts.model.ColorScale

/**
 * Grid heatmap. Each cell is filled per [colorScale] and bordered with a 1dp ink line.
 *
 * @param cells Row-major matrix of values (`cells[row][col]`). Rows may be variable
 *              length; missing cells render as transparent.
 * @param colorScale Maps cell value to fill color.
 * @param rowLabels Optional left-side labels (unused decoration today; reserved).
 * @param colLabels Optional bottom-side labels (unused decoration today; reserved).
 * @param modifier Layout modifier.
 */
@Composable
fun EurostatHeatmapChart(
    cells: List<List<Float>>,
    colorScale: ColorScale,
    rowLabels: List<String>? = null,
    colLabels: List<String>? = null,
    modifier: Modifier = Modifier,
) {
    if (cells.isEmpty()) {
        Box(modifier = modifier)
        return
    }
    val rows = cells.size
    val cols = cells.maxOfOrNull { it.size } ?: 0
    if (cols == 0) {
        Box(modifier = modifier)
        return
    }
    // Reserve names — silence unused parameter checks while keeping the public API stable.
    rowLabels?.size
    colLabels?.size

    // Hoist border color computation outside Canvas to avoid allocation on every draw pass.
    // Stroke is a value class and cheap, but the alpha-copy and Stroke creation is not.
    val borderColor = remember { ChartDefaults.Ink.copy(alpha = 0.25f) }

    Canvas(modifier = modifier) {
        val cellW = size.width / cols
        val cellH = size.height / rows
        val borderStroke = Stroke(width = ChartDefaults.ThinStrokeDp.toPx())
        for (r in 0 until rows) {
            val row = cells[r]
            for (c in 0 until cols) {
                if (c >= row.size) continue
                val v = row[c]
                val color = colorScale.colorFor(v)
                val tl = Offset(c * cellW, r * cellH)
                val sz = Size(cellW, cellH)
                drawRect(color = color, topLeft = tl, size = sz)
                drawRect(color = borderColor, topLeft = tl, size = sz, style = borderStroke)
            }
        }
    }
}

/** Preview composable for [EurostatHeatmapChart]. */
@Composable
fun EurostatHeatmapChartPreview() {
    val data = List(5) { r ->
        List(12) { c -> ((r + 1) * (c + 1) % 7).toFloat() }
    }
    val scale = ColorScale(
        domain = 0f..6f,
        from = Color(0xFFEDE6D6),
        to = ChartDefaults.Accent,
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(2.4f)
            .background(ChartDefaults.Paper)
    ) {
        EurostatHeatmapChart(cells = data, colorScale = scale, modifier = Modifier.fillMaxSize())
    }
}
