package eu.eurostat.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import eu.eurostat.ui.theme.Euro
import myeurostatapp.core_ui.generated.resources.Res
import myeurostatapp.core_ui.generated.resources.ui_footer_refresh_failed
import org.jetbrains.compose.resources.stringResource

/**
 * Footer line citing the Eurostat dataset and freshness status.
 *
 * Renders as: `● eurostat · {dataset} · {staleness}` in mono small text.
 * The leading dot turns warn-orange when [stale] or [refreshFailed] is true.
 * When [refreshFailed] is true the [staleness] segment is replaced by the
 * localized "refresh failed · showing saved data" text.
 *
 * @param dataset Eurostat dataset code, e.g. `"demo_pjan"`.
 * @param staleness human-readable freshness string, e.g. `"fresh · 2m"`,
 *   `"stale · 6h"`. Ignored while [refreshFailed] is true.
 * @param stale whether the data is currently stale; controls the dot color.
 * @param refreshFailed whether the last manual refresh failed while the data
 *   shown comes from cache; switches the dot to the warn color and shows the
 *   refresh-failed text instead of [staleness].
 */
@Composable
fun SourceFooter(
    dataset: String,
    staleness: String,
    stale: Boolean = false,
    refreshFailed: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val dotColor = if (stale || refreshFailed) Euro.colors.warn else Euro.colors.mutedAlt
    val status = if (refreshFailed) stringResource(Res.string.ui_footer_refresh_failed) else staleness
    Row(
        modifier = modifier.padding(vertical = Euro.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(dotColor, CircleShape),
        )
        Spacer(Modifier.width(Euro.spacing.s))
        Text(
            text = "eurostat · $dataset · $status",
            style = Euro.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = Euro.colors.muted,
        )
    }
}
