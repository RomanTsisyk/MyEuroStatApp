package eu.eurostat.core.charts.model

import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests the pure math in [ColorScale.colorFor] — domain clamping and endpoint
 * fidelity. Midpoint interpolation is only asserted to lie strictly between the
 * endpoints (Compose's [androidx.compose.ui.graphics.lerp] interpolates in a
 * perceptual color space, so exact channel values are not assumed).
 */
class ColorScaleTest {

    private val scale = ColorScale(domain = 0f..100f, from = Color.Black, to = Color.White)

    private fun assertChannelsClose(expected: Color, actual: Color, eps: Float = 0.03f) {
        assertTrue(abs(expected.red - actual.red) < eps, "red: ${expected.red} vs ${actual.red}")
        assertTrue(abs(expected.green - actual.green) < eps, "green: ${expected.green} vs ${actual.green}")
        assertTrue(abs(expected.blue - actual.blue) < eps, "blue: ${expected.blue} vs ${actual.blue}")
        assertTrue(abs(expected.alpha - actual.alpha) < eps, "alpha: ${expected.alpha} vs ${actual.alpha}")
    }

    @Test
    fun start_of_domain_yields_from_color() {
        assertChannelsClose(Color.Black, scale.colorFor(0f))
    }

    @Test
    fun end_of_domain_yields_to_color() {
        assertChannelsClose(Color.White, scale.colorFor(100f))
    }

    @Test
    fun values_below_domain_clamp_to_from() {
        assertEquals(scale.colorFor(0f), scale.colorFor(-50f))
    }

    @Test
    fun values_above_domain_clamp_to_to() {
        assertEquals(scale.colorFor(100f), scale.colorFor(200f))
    }

    @Test
    fun midpoint_lies_strictly_between_endpoints() {
        val mid = scale.colorFor(50f)
        assertTrue(mid.red > 0f && mid.red < 1f, "midpoint red should be between 0 and 1: ${mid.red}")
        assertTrue(mid != Color.Black && mid != Color.White)
    }

    @Test
    fun degenerate_domain_does_not_divide_by_zero() {
        val point = ColorScale(domain = 5f..5f, from = Color.Black, to = Color.White)
        // span is coerced to a tiny positive number; the point value maps to `from`.
        assertChannelsClose(Color.Black, point.colorFor(5f))
        // A value above the collapsed domain saturates to `to`.
        assertChannelsClose(Color.White, point.colorFor(6f))
    }
}
