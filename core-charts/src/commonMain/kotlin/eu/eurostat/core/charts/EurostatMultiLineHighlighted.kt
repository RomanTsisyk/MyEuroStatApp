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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import eu.eurostat.core.charts.internal.ChartDefaults
import eu.eurostat.core.charts.model.ChartPoint
import eu.eurostat.core.charts.model.ChartSeries

/**
 * Precomputed axis bounds for [EurostatMultiLineHighlighted], derived once per
 * `remember(series)` key change instead of on every recomposition.
 */
private data class MultiLineBounds(
    val xMin: Float,
    val yMin: Float,
    val xSpan: Float,
    val ySpan: Float,
)

/**
 * Multi-line chart where a single series is emphasized in its own accent and the rest
 * are dimmed to mutedAlt at low alpha — a "you among peers" frame.
 *
 * @param series All series, in legend order.
 * @param highlightIndex Index of the series to highlight. Out-of-range values render
 *                       all series in muted form.
 * @param modifier Layout modifier.
 */
@Composable
fun EurostatMultiLineHighlighted(
    series: List<ChartSeries>,
    highlightIndex: Int,
    modifier: Modifier = Modifier,
) {
    if (series.isEmpty() || series.all { it.points.isEmpty() }) {
        Box(modifier = modifier)
        return
    }
    val hasYValues = remember(series) { series.any { s -> s.points.any { it.y != null } } }
    if (!hasYValues) {
        Box(modifier = modifier)
        return
    }

    // Axis bounds are a pure derivation of series — hoisted so the min/max scans
    // over all points aren't repeated on every recomposition.
    val bounds = remember(series) {
        val allX = series.flatMap { s -> s.points.map { it.x.toFloat() } }
        val allY = series.flatMap { s -> s.points.mapNotNull { it.y?.toFloat() } }
        val xMin = allX.min()
        val xMax = allX.max()
        val yMin = allY.min()
        val yMax = allY.max()
        MultiLineBounds(
            xMin = xMin,
            yMin = yMin,
            xSpan = (xMax - xMin).coerceAtLeast(0.0001f),
            ySpan = (yMax - yMin).coerceAtLeast(0.0001f),
        )
    }
    val (xMin, yMin, xSpan, ySpan) = bounds

    // Hoist Path allocations outside Canvas to avoid per-frame GC pressure.
    val paths = remember(series.size) { List(series.size) { Path() } }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        series.forEachIndexed { idx, s ->
            val isHighlight = idx == highlightIndex
            val color = if (isHighlight) s.color else ChartDefaults.MutedAlt.copy(alpha = 0.55f)
            val strokeWidth = if (isHighlight) ChartDefaults.DefaultStrokeDp.toPx() else ChartDefaults.ThinStrokeDp.toPx()
            val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
            val path = paths[idx]
            path.reset()
            var started = false
            s.points.forEach { p ->
                val yv = p.y ?: run { started = false; return@forEach }
                val x = (p.x.toFloat() - xMin) / xSpan * w
                val y = h - (yv.toFloat() - yMin) / ySpan * h
                if (!started) {
                    path.moveTo(x, y)
                    started = true
                } else {
                    path.lineTo(x, y)
                }
            }
            drawPath(path = path, color = color, style = stroke)
        }
    }
}

/** Preview composable for [EurostatMultiLineHighlighted]. */
@Composable
fun EurostatMultiLineHighlightedPreview() {
    val years = (2010..2023).map { it.toDouble() }
    val countries = listOf("DE", "FR", "IT", "ES", "PL", "NL", "BE")
    val series = countries.mapIndexed { idx, code ->
        ChartSeries(
            label = code,
            color = ChartDefaults.Accent,
            points = years.mapIndexed { i, y -> ChartPoint(y, 40.0 + idx * 5 + i * 0.7) },
        )
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(2f)
            .background(ChartDefaults.Paper)
    ) {
        EurostatMultiLineHighlighted(
            series = series,
            highlightIndex = 0,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
