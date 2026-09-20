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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import eu.eurostat.core.charts.internal.ChartDefaults
import eu.eurostat.core.charts.internal.computeRadarLayout
import eu.eurostat.core.charts.internal.radarLabelOffset
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * A single named series for an [EurostatRadarChart].
 *
 * @property label Series label.
 * @property color Stroke / fill color for this series.
 * @property values Normalized values in 0..1, one per axis, in the same order as the
 *                  parent chart's `axes` list.
 */
data class RadarSeries(
    val label: String,
    val color: Color,
    val values: List<Float>,
)

/**
 * Radar (spider) chart with three or more axes and dashed concentric guide rings.
 *
 * Each series's `values` list must have the same size as [axes]; otherwise that
 * series is skipped.
 *
 * @param axes Axis labels, one per spoke. Drawn at the spoke tips only when
 *             [showAxisLabels] is true; otherwise they are decorative.
 * @param series Series to overlay. Each value is expected to be normalized to 0..1.
 * @param modifier Layout modifier.
 * @param showAxisLabels When true, draws each axis label just outside its spoke
 *                       tip and shrinks the polygon so the labels stay inside the
 *                       canvas. Defaults to false (no labels, full-size polygon).
 */
@Composable
fun EurostatRadarChart(
    axes: List<String>,
    series: List<RadarSeries>,
    modifier: Modifier = Modifier,
    showAxisLabels: Boolean = false,
) {
    if (axes.size < 3) {
        Box(modifier = modifier)
        return
    }
    // One shared path reused for each ring and series to avoid per-frame allocation.
    val sharedPath = remember { Path() }

    // Per-axis spoke angle only depends on the axis count — hoisted so it isn't
    // recomputed with trig calls on every ring/spoke/series iteration below.
    val axisAngles = remember(axes.size) {
        val n = axes.size
        FloatArray(n) { i -> (-PI / 2 + i * 2 * PI / n).toFloat() }
    }

    val textMeasurer = rememberTextMeasurer()

    Canvas(modifier = modifier) {
        val n = axes.size
        // Labels are measured single-line, ellipsized at 40% of the canvas width.
        val labelLayouts: List<TextLayoutResult> = if (showAxisLabels) {
            val maxLabelWidth = (size.width * 0.4f).toInt().coerceAtLeast(1)
            axes.map { label ->
                textMeasurer.measure(
                    text = label,
                    style = ChartDefaults.CategoryLabelStyle,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    constraints = Constraints(maxWidth = maxLabelWidth),
                )
            }
        } else {
            emptyList()
        }
        val labelGap = ChartDefaults.AxisLabelGapDp.toPx()
        val layout = computeRadarLayout(
            width = size.width,
            height = size.height,
            angles = axisAngles,
            labelWidths = FloatArray(labelLayouts.size) { labelLayouts[it].size.width.toFloat() },
            labelHeights = FloatArray(labelLayouts.size) { labelLayouts[it].size.height.toFloat() },
            gap = labelGap,
        )
        val cx = layout.centerX
        val cy = layout.centerY
        val radius = layout.radius
        val ringColor = ChartDefaults.Ink.copy(alpha = 0.18f)
        val spokeColor = ChartDefaults.Ink.copy(alpha = 0.12f)
        val dash = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
        val ringSteps = 4
        val ringStroke = Stroke(width = ChartDefaults.ThinStrokeDp.toPx(), pathEffect = dash)
        for (s in 1..ringSteps) {
            val r = radius * s / ringSteps
            sharedPath.reset()
            for (i in 0 until n) {
                val angle = axisAngles[i]
                val x = cx + r * cos(angle)
                val y = cy + r * sin(angle)
                if (i == 0) sharedPath.moveTo(x, y) else sharedPath.lineTo(x, y)
            }
            sharedPath.close()
            drawPath(path = sharedPath, color = ringColor, style = ringStroke)
        }
        for (i in 0 until n) {
            val angle = axisAngles[i]
            drawLine(
                color = spokeColor,
                start = Offset(cx, cy),
                end = Offset(cx + radius * cos(angle), cy + radius * sin(angle)),
                strokeWidth = ChartDefaults.ThinStrokeDp.toPx(),
            )
        }
        series.forEach { s ->
            if (s.values.size != n) return@forEach
            sharedPath.reset()
            for (i in 0 until n) {
                val v = s.values[i].coerceIn(0f, 1f)
                val angle = axisAngles[i]
                val r = radius * v
                val x = cx + r * cos(angle)
                val y = cy + r * sin(angle)
                if (i == 0) sharedPath.moveTo(x, y) else sharedPath.lineTo(x, y)
            }
            sharedPath.close()
            drawPath(path = sharedPath, color = s.color.copy(alpha = 0.18f))
            drawPath(
                path = sharedPath,
                color = s.color,
                style = Stroke(width = ChartDefaults.DefaultStrokeDp.toPx()),
            )
        }
        labelLayouts.forEachIndexed { i, label ->
            val o = radarLabelOffset(
                radius = radius,
                angle = axisAngles[i],
                labelWidth = label.size.width.toFloat(),
                labelHeight = label.size.height.toFloat(),
                gap = labelGap,
            )
            drawText(textLayoutResult = label, topLeft = Offset(cx + o.x, cy + o.y))
        }
    }
}

/** Preview composable for [EurostatRadarChart]. */
@Composable
fun EurostatRadarChartPreview() {
    val axes = listOf("Economy", "Environment", "Social", "Science", "Transport")
    val series = listOf(
        RadarSeries("DE", ChartDefaults.Accent, listOf(0.8f, 0.7f, 0.6f, 0.9f, 0.75f)),
        RadarSeries("PL", ChartDefaults.AccentSecondary, listOf(0.5f, 0.4f, 0.55f, 0.6f, 0.5f)),
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(ChartDefaults.Paper)
    ) {
        EurostatRadarChart(
            axes = axes,
            series = series,
            modifier = Modifier.fillMaxSize(),
            showAxisLabels = true,
        )
    }
}
