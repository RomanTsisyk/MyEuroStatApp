package eu.eurostat.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import eu.eurostat.ui.theme.Euro

/**
 * Rounded card following the hi-fi Eurostat aesthetic.
 *
 * Default appearance: 24dp rounded corners, white `paperAlt` background (warm
 * surface in dark theme), 1dp translucent hairline border, [Euro.spacing.base]
 * inner padding.
 *
 * @param dashed when true, the border is rendered as a dashed stroke for
 *   wireframe-like accent surfaces.
 * @param accent optional 3dp top stripe — used to mark a card as belonging to
 *   a specific module (matches `HFCard accent` in the hi-fi spec).
 */
@Composable
fun EuroCard(
    modifier: Modifier = Modifier,
    dashed: Boolean = false,
    accent: androidx.compose.ui.graphics.Color? = null,
    content: @Composable () -> Unit,
) {
    val shape: RoundedCornerShape = Euro.shapes.card
    val borderColor = if (dashed) Euro.colors.ink else Euro.colors.border
    val bg = Euro.colors.paperAlt

    val borderModifier = if (dashed) {
        Modifier.drawBehind {
            val outline: Outline = shape.createOutline(size, LayoutDirection.Ltr, this)
            drawOutline(
                outline = outline,
                color = borderColor,
                style = Stroke(
                    width = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(6.dp.toPx(), 4.dp.toPx()),
                        0f,
                    ),
                ),
            )
        }
    } else {
        Modifier.border(width = 1.dp, color = borderColor, shape = shape)
    }

    val accentModifier = if (accent != null) {
        Modifier.drawBehind {
            val stripe = 3.dp.toPx()
            drawRect(
                color = accent,
                topLeft = androidx.compose.ui.geometry.Offset.Zero,
                size = androidx.compose.ui.geometry.Size(size.width, stripe),
            )
        }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(bg, shape)
            .then(borderModifier)
            .then(accentModifier)
            .padding(Euro.spacing.base),
    ) {
        content()
    }
}
