package eu.eurostat.ui.component.states

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import eu.eurostat.ui.theme.Euro

/**
 * Generic empty state. Renders a dashed-border square icon placeholder, a
 * headline and a body paragraph, optionally followed by a primary action
 * button.
 *
 * @param headline short title rendered in headlineLarge.
 * @param body supporting copy rendered in bodyMedium muted.
 * @param actionLabel when non-null together with [onAction], renders a
 *   filled action button below the body.
 * @param onAction callback for the action button.
 */
@Composable
fun EmptyState(
    headline: String,
    body: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val strokeColor = Euro.colors.mutedAlt
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Euro.spacing.l),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Canvas(modifier = Modifier.size(72.dp)) {
            drawRoundRect(
                color = strokeColor,
                cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                style = Stroke(
                    width = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(6.dp.toPx(), 4.dp.toPx()),
                        0f,
                    ),
                ),
            )
        }
        Spacer(Modifier.height(Euro.spacing.base))
        Text(
            text = headline,
            style = Euro.typography.headlineLarge,
            color = Euro.colors.ink,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Euro.spacing.s))
        Text(
            text = body,
            style = Euro.typography.bodyMedium,
            color = Euro.colors.muted,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(Euro.spacing.base))
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Euro.colors.accent,
                    contentColor = androidx.compose.ui.graphics.Color.White,
                ),
            ) {
                Text(actionLabel, style = Euro.typography.labelLarge)
            }
        }
    }
}
