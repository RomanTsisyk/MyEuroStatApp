package eu.eurostat.ui.component.states

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import eu.eurostat.ui.theme.Euro

/**
 * Replaces the legacy `ErrorView` from `core.ui.components`. Centered layout
 * with a headline, body and a primary "retry" button.
 *
 * @param headline short error title.
 * @param body optional supporting copy explaining the failure.
 * @param onRetry when non-null, renders the retry button.
 */
@Composable
fun ErrorState(
    headline: String,
    modifier: Modifier = Modifier,
    body: String = "",
    onRetry: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Euro.spacing.l),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = headline,
            style = Euro.typography.headlineLarge,
            color = Euro.colors.ink,
            textAlign = TextAlign.Center,
        )
        if (body.isNotBlank()) {
            Spacer(Modifier.height(Euro.spacing.s))
            Text(
                text = body,
                style = Euro.typography.bodyMedium,
                color = Euro.colors.muted,
                textAlign = TextAlign.Center,
            )
        }
        if (onRetry != null) {
            Spacer(Modifier.height(Euro.spacing.base))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Euro.colors.accent,
                    contentColor = androidx.compose.ui.graphics.Color.White,
                ),
            ) {
                Text("retry", style = Euro.typography.labelLarge)
            }
        }
    }
}
