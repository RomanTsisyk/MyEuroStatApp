package eu.eurostat.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import eu.eurostat.ui.theme.Euro
import myeurostatapp.core_ui.generated.resources.Res
import myeurostatapp.core_ui.generated.resources.ui_chip_add
import myeurostatapp.core_ui.generated.resources.ui_chip_add_country
import org.jetbrains.compose.resources.stringResource

/**
 * Horizontally-scrolling row of [CountryChip]s with a dashed "+ add" chip at
 * the end of the list.
 *
 * @param countries the list of two-letter ISO codes to render.
 * @param active codes that should be displayed as selected.
 * @param onSelect invoked when a country chip is tapped (toggle selection).
 * @param onAdd invoked when the trailing dashed "+ add" chip is tapped.
 */
@Composable
fun CountryChipsRow(
    countries: List<String>,
    active: Set<String>,
    onSelect: (String) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = Euro.spacing.base),
        horizontalArrangement = Arrangement.spacedBy(Euro.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items(countries) { code ->
            CountryChip(
                code = code,
                selected = code in active,
                onClick = { onSelect(code) },
            )
        }
        item { AddCountryChip(onClick = onAdd) }
    }
}

@Composable
private fun AddCountryChip(onClick: () -> Unit) {
    val shape = Euro.shapes.pill
    val strokeColor = Euro.colors.mutedAlt
    Box(
        modifier = Modifier
            .defaultMinSize(minHeight = 48.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(28.dp)
            .drawBehind {
                val outline: Outline = shape.createOutline(size, LayoutDirection.Ltr, this)
                drawOutline(
                    outline = outline,
                    color = strokeColor,
                    style = Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(5.dp.toPx(), 3.dp.toPx()),
                            0f,
                        ),
                    ),
                )
            }
            .padding(horizontal = Euro.spacing.m, vertical = Euro.spacing.xs),
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = stringResource(Res.string.ui_chip_add_country),
            tint = Euro.colors.muted,
            modifier = Modifier.padding(0.dp),
        )
        Text(
            text = stringResource(Res.string.ui_chip_add),
            style = Euro.typography.labelLarge,
            color = Euro.colors.muted,
            modifier = Modifier.padding(start = Euro.spacing.xs),
        )
    }
    }
}
