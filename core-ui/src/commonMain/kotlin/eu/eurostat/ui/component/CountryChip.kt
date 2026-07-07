package eu.eurostat.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.eurostat.core.common.flagFor
import eu.eurostat.ui.theme.Euro
import myeurostatapp.core_ui.generated.resources.Res
import myeurostatapp.core_ui.generated.resources.ui_chip_remove_country
import org.jetbrains.compose.resources.stringResource

/**
 * Pill-shaped country selector chip.
 *
 * Renders the Unicode flag emoji for the country (via [flagFor]) followed by
 * the country code label. When selected the chip is filled with ink; when not,
 * it uses an outlined treatment. An optional close button appears at the trailing
 * edge.
 *
 * @param code Eurostat country code (e.g. `"DE"`, `"EL"`, `"EU27_2020"`).
 * @param selected whether the chip is in the selected state.
 * @param onClick fires when the chip body is tapped.
 * @param onClose if non-null, renders a close (x) icon and invokes the
 *   callback when tapped.
 */
@Composable
fun CountryChip(
    code: String,
    selected: Boolean,
    onClick: () -> Unit,
    onClose: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val shape: RoundedCornerShape = Euro.shapes.pill
    val bg = if (selected) Euro.colors.ink else Euro.colors.paperAlt
    val fg = if (selected) Euro.colors.paper else Euro.colors.ink
    val borderColor = if (selected) Euro.colors.ink else Euro.colors.border
    val flag = flagFor(code)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(shape)
            .background(bg, shape)
            .border(1.dp, borderColor, shape)
            .clickable(onClick = onClick)
            .defaultMinSize(minHeight = 48.dp)
            .padding(horizontal = Euro.spacing.m, vertical = Euro.spacing.xs),
    ) {
        Text(
            text = flag,
            fontSize = 14.sp,
            lineHeight = 14.sp,
        )
        Spacer(Modifier.width(Euro.spacing.s))
        Text(
            text = code.uppercase(),
            style = Euro.typography.labelLarge,
            color = fg,
        )
        if (onClose != null) {
            Spacer(Modifier.width(Euro.spacing.xs))
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(Res.string.ui_chip_remove_country, code),
                    tint = fg,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}
