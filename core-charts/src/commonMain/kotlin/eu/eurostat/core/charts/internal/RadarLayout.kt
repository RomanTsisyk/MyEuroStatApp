package eu.eurostat.core.charts.internal

import androidx.compose.ui.geometry.Offset
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** Fraction of the half-extent of the canvas used by the radar polygon when no labels are drawn. */
private const val RadarRadiusFraction = 0.9f

/** Number of shrink steps tried when fitting axis labels inside the canvas. */
private const val RadarFitSteps = 40

/**
 * Geometry of a radar chart: where the centre sits and how long each spoke is.
 *
 * @property centerX Centre X in canvas pixels.
 * @property centerY Centre Y in canvas pixels.
 * @property radius Spoke length (outermost ring) in pixels.
 */
internal data class RadarLayout(
    val centerX: Float,
    val centerY: Float,
    val radius: Float,
)

/**
 * Top-left corner, relative to the chart centre, of the label box belonging to
 * the spoke at [angle]. The box is pushed [gap] pixels beyond the spoke tip in
 * the direction of the spoke, so the label never overlaps the polygon.
 */
internal fun radarLabelOffset(
    radius: Float,
    angle: Float,
    labelWidth: Float,
    labelHeight: Float,
    gap: Float,
): Offset {
    val ux = cos(angle)
    val uy = sin(angle)
    return Offset(
        x = (radius + gap) * ux + ux * labelWidth / 2f - labelWidth / 2f,
        y = (radius + gap) * uy + uy * labelHeight / 2f - labelHeight / 2f,
    )
}

/**
 * Computes the radar geometry for a [width] x [height] canvas.
 *
 * Without labels ([labelWidths] empty) the polygon is centred and uses 90% of
 * the smaller half-extent. With labels the radius is shrunk (in small steps)
 * until the polygon plus every label box fits inside the canvas, and the
 * centre is shifted so that the whole composition is centred.
 *
 * @param angles Spoke angles in radians, one per axis.
 * @param labelWidths Measured label widths in pixels (same order as [angles]); empty for no labels.
 * @param labelHeights Measured label heights in pixels (same order as [angles]); empty for no labels.
 * @param gap Distance in pixels between a spoke tip and its label.
 */
internal fun computeRadarLayout(
    width: Float,
    height: Float,
    angles: FloatArray,
    labelWidths: FloatArray,
    labelHeights: FloatArray,
    gap: Float,
): RadarLayout {
    val fullRadius = min(width, height) / 2f * RadarRadiusFraction
    if (labelWidths.isEmpty()) return RadarLayout(width / 2f, height / 2f, fullRadius)

    for (step in RadarFitSteps downTo 0) {
        val r = fullRadius * step / RadarFitSteps
        // Bounding box of the polygon and all labels, relative to the centre.
        var minX = 0f
        var maxX = 0f
        var minY = 0f
        var maxY = 0f
        for (i in angles.indices) {
            val tipX = r * cos(angles[i])
            val tipY = r * sin(angles[i])
            minX = min(minX, tipX)
            maxX = max(maxX, tipX)
            minY = min(minY, tipY)
            maxY = max(maxY, tipY)
            val o = radarLabelOffset(r, angles[i], labelWidths[i], labelHeights[i], gap)
            minX = min(minX, o.x)
            maxX = max(maxX, o.x + labelWidths[i])
            minY = min(minY, o.y)
            maxY = max(maxY, o.y + labelHeights[i])
        }
        if (maxX - minX <= width && maxY - minY <= height) {
            return RadarLayout(
                centerX = width / 2f - (minX + maxX) / 2f,
                centerY = height / 2f - (minY + maxY) / 2f,
                radius = r,
            )
        }
    }
    return RadarLayout(width / 2f, height / 2f, 0f)
}
