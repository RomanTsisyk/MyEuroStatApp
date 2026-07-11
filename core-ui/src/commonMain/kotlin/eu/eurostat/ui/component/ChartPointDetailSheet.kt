package eu.eurostat.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import eu.eurostat.ui.theme.Euro
import kotlinx.coroutines.launch
import myeurostatapp.core_ui.generated.resources.Res
import myeurostatapp.core_ui.generated.resources.ui_chart_detail_close
import myeurostatapp.core_ui.generated.resources.ui_chart_detail_dataset
import myeurostatapp.core_ui.generated.resources.ui_chart_detail_unit
import myeurostatapp.core_ui.generated.resources.ui_chart_detail_value
import myeurostatapp.core_ui.generated.resources.ui_chart_detail_year
import org.jetbrains.compose.resources.stringResource

/**
 * Detail sheet shown when a chart data point is tapped (see the line chart's
 * `onPointTap`). Presents the tapped observation as a [ModalBottomSheet]: the
 * series label as the title, then labelled rows for year, value, unit and the
 * source dataset code, plus a localized "close" affordance.
 *
 * All value strings are passed **already resolved** so the feature module owns
 * formatting (metric-specific number formatting, locale-aware separators, and
 * the correct dataset code) — this component only lays them out and localizes
 * the row *labels*.
 *
 * @param seriesLabel Title of the sheet, typically the country/series name.
 * @param year Pre-formatted year of the tapped point (e.g. `"2021"`).
 * @param value Pre-formatted value in the screen's display unit (e.g. `"3,451"`).
 * @param unit Short unit label for [value] (e.g. `"B€"`, `"% of GDP"`).
 * @param datasetCode Eurostat dataset code the value is sourced from (e.g. `"nama_10_gdp"`).
 * @param onDismiss Called when the sheet is dismissed (swipe, scrim, or "close").
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartPointDetailSheet(
    seriesLabel: String,
    year: String,
    value: String,
    unit: String,
    datasetCode: String,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    val coroutineScope = rememberCoroutineScope()

    // Swipe/scrim dismissal already animates via onDismissRequest = onDismiss.
    // Route the explicit "Close" button through the same collapse animation
    // by hiding the sheet first and only calling onDismiss (which removes
    // this composable) once that animation completes.
    val animatedDismiss: () -> Unit = {
        coroutineScope.launch { sheetState.hide() }
            .invokeOnCompletion { onDismiss() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(
                    horizontal = Euro.spacing.base,
                    vertical = Euro.spacing.s,
                ),
        ) {
            Text(
                text = seriesLabel,
                style = Euro.typography.headlineSmall,
                color = Euro.colors.ink,
            )

            Spacer(Modifier.height(Euro.spacing.s))
            HorizontalDivider()
            Spacer(Modifier.height(Euro.spacing.s))

            DetailRow(label = stringResource(Res.string.ui_chart_detail_year), value = year)
            DetailRow(label = stringResource(Res.string.ui_chart_detail_value), value = value)
            DetailRow(label = stringResource(Res.string.ui_chart_detail_unit), value = unit)
            DetailRow(label = stringResource(Res.string.ui_chart_detail_dataset), value = datasetCode)

            Spacer(Modifier.height(Euro.spacing.s))

            TextButton(
                onClick = animatedDismiss,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(Res.string.ui_chart_detail_close),
                    style = Euro.typography.bodyMedium,
                    color = Euro.colors.accent,
                )
            }
        }
    }
}

/**
 * One `label · value` line of the detail sheet: muted left label, ink-colored
 * right value that wraps/aligns to the end so long dataset codes stay readable.
 */
@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Euro.spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(Euro.spacing.m),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            style = Euro.typography.bodySmall,
            color = Euro.colors.muted,
        )
        Text(
            text = value,
            style = Euro.typography.bodyMedium,
            color = Euro.colors.ink,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f),
        )
    }
}
