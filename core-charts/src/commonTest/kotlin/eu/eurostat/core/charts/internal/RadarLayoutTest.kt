package eu.eurostat.core.charts.internal

import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Tests for [computeRadarLayout] and [radarLabelOffset]. */
class RadarLayoutTest {

    private val triangle = FloatArray(3) { i -> (-PI / 2 + i * 2 * PI / 3).toFloat() }

    @Test
    fun without_labels_polygon_is_centred_at_ninety_percent_of_half_extent() {
        val layout = computeRadarLayout(
            width = 400f,
            height = 300f,
            angles = triangle,
            labelWidths = FloatArray(0),
            labelHeights = FloatArray(0),
            gap = 4f,
        )
        assertEquals(200f, layout.centerX, 0.001f)
        assertEquals(150f, layout.centerY, 0.001f)
        assertEquals(135f, layout.radius, 0.001f)
    }

    @Test
    fun with_labels_every_label_box_stays_inside_the_canvas() {
        val width = 300f
        val height = 200f
        val labelW = floatArrayOf(60f, 50f, 70f)
        val labelH = floatArrayOf(14f, 14f, 14f)
        val gap = 4f
        val layout = computeRadarLayout(width, height, triangle, labelW, labelH, gap)

        assertTrue(layout.radius > 0f)
        for (i in triangle.indices) {
            val o = radarLabelOffset(layout.radius, triangle[i], labelW[i], labelH[i], gap)
            val left = layout.centerX + o.x
            val top = layout.centerY + o.y
            assertTrue(left >= -0.01f, "label $i left=$left")
            assertTrue(top >= -0.01f, "label $i top=$top")
            assertTrue(left + labelW[i] <= width + 0.01f, "label $i right")
            assertTrue(top + labelH[i] <= height + 0.01f, "label $i bottom")
        }
    }

    @Test
    fun labels_shrink_the_polygon_when_canvas_is_tight() {
        val unlabelled = computeRadarLayout(200f, 80f, triangle, FloatArray(0), FloatArray(0), 4f)
        val labelled = computeRadarLayout(
            width = 200f,
            height = 80f,
            angles = triangle,
            labelWidths = floatArrayOf(60f, 60f, 60f),
            labelHeights = floatArrayOf(14f, 14f, 14f),
            gap = 4f,
        )
        assertTrue(labelled.radius < unlabelled.radius)
    }

    @Test
    fun top_label_is_centred_horizontally_above_the_spoke_tip() {
        val o = radarLabelOffset(
            radius = 100f,
            angle = triangle[0],
            labelWidth = 40f,
            labelHeight = 10f,
            gap = 4f,
        )
        assertEquals(-20f, o.x, 0.01f)
        assertEquals(-114f, o.y, 0.01f)
    }
}
