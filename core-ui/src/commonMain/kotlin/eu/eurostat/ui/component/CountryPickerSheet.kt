package eu.eurostat.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import eu.eurostat.core.common.EurostatCountries
import eu.eurostat.ui.theme.Euro
import myeurostatapp.core_ui.generated.resources.Res
import myeurostatapp.core_ui.generated.resources.ui_country_picker_apply
import myeurostatapp.core_ui.generated.resources.ui_country_picker_search_label
import myeurostatapp.core_ui.generated.resources.ui_country_picker_title
import org.jetbrains.compose.resources.stringResource

/**
 * Multi-select country picker presented as a [ModalBottomSheet].
 *
 * Shows a search [OutlinedTextField] that filters [EurostatCountries.ALL] by name
 * or code, a [LazyColumn] of rows with a flag/code badge, name, and [Checkbox], and
 * a primary "Apply" button that calls [onConfirm] with the locally accumulated
 * selection. Enforces [maxSelection]: additional checkboxes are disabled when the
 * limit is reached.
 *
 * @param selected      Initially selected country codes.
 * @param onConfirm     Called with the final set of codes when the user taps "Apply".
 * @param onDismiss     Called when the sheet is dismissed without confirming.
 * @param title         Sheet header label.
 * @param maxSelection  Maximum number of simultaneously selected countries.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountryPickerSheet(
    selected: Set<String>,
    onConfirm: (Set<String>) -> Unit,
    onDismiss: () -> Unit,
    title: String = stringResource(Res.string.ui_country_picker_title),
    maxSelection: Int = 5,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Local mutable selection — seeded from the caller's current selection.
    var selectedLocal by remember(selected) { mutableStateOf(selected) }
    var query by remember { mutableStateOf("") }

    val filtered = remember(query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) {
            EurostatCountries.ALL
        } else {
            EurostatCountries.ALL.filter { country ->
                country.code.lowercase().contains(q) || country.name.lowercase().contains(q)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(bottom = Euro.spacing.base),
        ) {
            Text(
                text = title,
                style = Euro.typography.headlineSmall,
                color = Euro.colors.ink,
                modifier = Modifier.padding(
                    horizontal = Euro.spacing.base,
                    vertical = Euro.spacing.s,
                ),
            )

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text(stringResource(Res.string.ui_country_picker_search_label)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Euro.spacing.base)
                    .padding(bottom = Euro.spacing.s),
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
            ) {
                items(filtered, key = { it.code }) { country ->
                    val isChecked = country.code in selectedLocal
                    val atLimit = selectedLocal.size >= maxSelection && !isChecked

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = Euro.spacing.base,
                                vertical = Euro.spacing.xs,
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Flag / code badge
                        Text(
                            text = country.flag,
                            style = Euro.typography.bodySmall,
                            color = if (atLimit) Euro.colors.muted else Euro.colors.ink,
                            modifier = Modifier.width(Euro.spacing.xl),
                        )

                        Spacer(Modifier.width(Euro.spacing.s))

                        Text(
                            text = country.name,
                            style = Euro.typography.bodyMedium,
                            color = if (atLimit) Euro.colors.muted else Euro.colors.ink,
                            modifier = Modifier.weight(1f),
                        )

                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { checked ->
                                selectedLocal = if (checked) {
                                    selectedLocal + country.code
                                } else {
                                    selectedLocal - country.code
                                }
                            },
                            enabled = !atLimit,
                            colors = CheckboxDefaults.colors(
                                checkedColor = Euro.colors.accent,
                            ),
                        )
                    }

                    HorizontalDivider()
                }
            }

            Spacer(Modifier.height(Euro.spacing.s))

            Button(
                onClick = { onConfirm(selectedLocal) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Euro.spacing.base),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Euro.colors.accent,
                ),
            ) {
                Text(
                    text = stringResource(Res.string.ui_country_picker_apply, selectedLocal.size),
                    style = Euro.typography.bodyMedium,
                )
            }
        }
    }
}
