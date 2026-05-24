package eu.eurostat.core.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import eu.eurostat.core.charts.internal.ChartDefaults
import eu.eurostat.core.charts.model.ChartAxis
import eu.eurostat.core.charts.model.ChartPoint
import eu.eurostat.core.charts.model.ChartSeries

/**
 * Editorial multi-series line chart rendered on Compose Canvas.
 *
 * Renders dashed horizontal gridlines with tabular axis labels.
 * Null y-values are honored as gaps when [showGaps] is true; otherwise the
 * surrounding non-null points are connected through the gap.
 *
 * @param series Series to plot. Empty list renders an empty box.
 * @param xAxis Horizontal axis (range optional; auto-fits if absent).
 * @param yAxis Vertical axis (range optional; auto-fits if absent).
 * @param hideAxis When true, suppresses axis labels and gutter.
 * @param showGaps When true, null y values break the line.
 * @param modifier Layout modifier.
 */
@Composable
fun EurostatLineChart(
    series: List<ChartSeries>,
    xAxis: ChartAxis,
    yAxis: ChartAxis,
    hideAxis: Boolean = false,
    showGaps: Boolean = true,
    modifier: Modifier = Modifier,
) {
    if (series.isEmpty() || series.all { it.points.isEmpty() }) {
        Box(modifier = modifier)
        return
    }

    val allPoints = series.flatMap { it.points }
    val xValues = allPoints.map { it.x.toFloat() }
    val yValues = allPoints.mapNotNull { it.y?.toFloat() }
    if (yValues.isEmpty()) {
        Box(modifier = modifier)
        return
    }

    val xMin = xAxis.range?.start ?: xValues.min()
    val xMax = xAxis.range?.endInclusive ?: xValues.max()
    val yMin = yAxis.range?.start ?: yValues.min()
    val yMax = yAxis.range?.endInclusive ?: yValues.max()
    val xSpan = (xMax - xMin).coerceAtLeast(0.0001f)
    val ySpan = (yMax - yMin).coerceAtLeast(0.0001f)

    val gutter = if (hideAxis) 0.dp else ChartDefaults.AxisGutterDp
    val tickCount = 4
    val yTicks = (0..tickCount).map { i -> yMin + ySpan * i / tickCount }
    val xTicks = listOf(xMin, xMax)

    // Hoist Path allocation outside Canvas to avoid per-frame GC pressure.
    val paths = remember(series.size) { List(series.size) { Path() } }

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = gutter, bottom = gutter)
        ) {
            val w = size.width
            val h = size.height
            val gridColor = ChartDefaults.GridLine
            val dash = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
            yTicks.forEach { yv ->
                val y = h - (yv - yMin) / ySpan * h
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1f,
                    pathEffect = dash,
                )
            }

            val stroke = Stroke(
                width = ChartDefaults.DefaultStrokeDp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            )
            series.forEachIndexed { sIdx, s ->
                val path = paths[sIdx]
                path.reset()
                var started = false
                s.points.forEach { p ->
                    val yv = p.y
                    if (yv == null) {
                        if (showGaps) started = false
                        return@forEach
                    }
                    val x = (p.x.toFloat() - xMin) / xSpan * w
                    val y = h - (yv.toFloat() - yMin) / ySpan * h
                    if (!started) {
                        path.moveTo(x, y)
                        started = true
                    } else {
                        path.lineTo(x, y)
                    }
                }
                drawPath(path = path, color = s.color, style = stroke)
            }
        }

        if (!hideAxis) {
            AxisLabelsOverlay(
                xAxis = xAxis,
                yAxis = yAxis,
                xTicks = xTicks,
                yTicks = yTicks,
                yMin = yMin,
                ySpan = ySpan,
                gutter = gutter,
            )
        }
    }
}

@Composable
private fun AxisLabelsOverlay(
    xAxis: ChartAxis,
    yAxis: ChartAxis,
    xTicks: List<Float>,
    yTicks: List<Float>,
    yMin: Float,
    ySpan: Float,
    gutter: androidx.compose.ui.unit.Dp,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val chartH = maxHeight - gutter  // chart canvas height (without X-axis gutter)
        yTicks.forEach { v ->
            // Fraction from 0 (bottom) to 1 (top), mapped to vertical position in the chart area.
            val frac = ((v - yMin) / ySpan).coerceIn(0f, 1f)
            val yOffset = chartH * (1f - frac)
            Text(
                text = yAxis.formatter(v),
                style = ChartDefaults.AxisLabelStyle,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = 0.dp, y = yOffset),
            )
        }
        // Compact label row at bottom-left / bottom-right
        Text(
            text = xAxis.formatter(xTicks.first()),
            style = ChartDefaults.AxisLabelStyle,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = gutter),
        )
        Text(
            text = xAxis.formatter(xTicks.last()),
            style = ChartDefaults.AxisLabelStyle,
            modifier = Modifier.align(Alignment.BottomEnd),
        )
    }
}

/**
 * Back-compatibility overload preserving the original (label-only) signature used
 * by existing feature modules before [ChartAxis] was introduced.
 */
@Composable
fun EurostatLineChart(
    series: List<ChartSeries>,
    modifier: Modifier = Modifier,
    xAxisLabel: String = "",
    yAxisLabel: String = "",
) {
    EurostatLineChart(
        series = series,
        xAxis = ChartAxis(label = xAxisLabel),
        yAxis = ChartAxis(label = yAxisLabel),
        modifier = modifier,
    )
}

/** Preview composable using two sample country series across years. */
@Composable
fun EurostatLineChartPreview() {
    val years = (2010..2023).map { it.toDouble() }
    val a = ChartSeries(
        label = "DE",
        color = ChartDefaults.Accent,
        points = years.mapIndexed { i, y -> ChartPoint(y, 80.0 + i * 0.4) },
    )
    val b = ChartSeries(
        label = "FR",
        color = ChartDefaults.AccentSecondary,
        points = years.mapIndexed { i, y -> ChartPoint(y, 65.0 + i * 0.6) },
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(2f)
            .background(ChartDefaults.Paper)
    ) {
        EurostatLineChart(
            series = listOf(a, b),
            xAxis = ChartAxis(label = "Year"),
            yAxis = ChartAxis(label = "Population (M)"),
        )
    }
}
