package eu.eurostat.core.charts.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/**
 * Linear color scale mapping a numeric domain to an interpolated color.
 *
 * @property domain Inclusive value range that maps from [from] to [to].
 * @property from Color emitted for values at (or below) [ClosedFloatingPointRange.start].
 * @property to Color emitted for values at (or above) [ClosedFloatingPointRange.endInclusive].
 */
data class ColorScale(
    val domain: ClosedFloatingPointRange<Float>,
    val from: Color,
    val to: Color,
) {
    /**
     * Returns the interpolated color for [value], clamped to the domain.
     */
    fun colorFor(value: Float): Color {
        val span = (domain.endInclusive - domain.start).coerceAtLeast(0.0001f)
        val t = ((value - domain.start) / span).coerceIn(0f, 1f)
        return lerp(from, to, t)
    }
}
