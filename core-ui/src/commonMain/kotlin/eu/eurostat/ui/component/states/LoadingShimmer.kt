package eu.eurostat.ui.component.states

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import eu.eurostat.ui.theme.Euro

/**
 * Chart-shaped skeleton loader with an infinite shimmer sweep. Use anywhere
 * the UI is waiting on a chart to render. The component renders as a
 * rounded rectangle of the requested [height] with a paperAlt base and a
 * paper-tinted gradient that sweeps left-to-right.
 *
 * @param height visible height of the skeleton; default approximates a
 *   compact chart panel.
 */
@Composable
fun LoadingShimmer(
    modifier: Modifier = Modifier,
    height: Dp = 180.dp,
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translate by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer-translate",
    )

    // surface2 (#EAE6DD) as the base tile; paperAlt (#FFFFFF) as the sweep
    // highlight. Using colorStops with tight 0.3..0.5..0.7 offsets produces a
    // sharp moving band that is clearly visible against the paper background,
    // while remaining within the warm-tone palette (no grey or blue shimmer).
    val base = Euro.colors.surface2
    val highlight = Euro.colors.paperAlt

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(Euro.shapes.medium)
            .background(base, Euro.shapes.medium),
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(height)) {
            val width = size.width
            val sweepWidth = width * 0.5f
            val startX = -sweepWidth + (width + sweepWidth) * translate
            val brush = Brush.linearGradient(
                colorStops = arrayOf(
                    0.0f to base,
                    0.3f to base,
                    0.5f to highlight,
                    0.7f to base,
                    1.0f to base,
                ),
                start = Offset(startX, 0f),
                end = Offset(startX + sweepWidth, 0f),
            )
            drawRect(brush = brush)
        }
    }
}
