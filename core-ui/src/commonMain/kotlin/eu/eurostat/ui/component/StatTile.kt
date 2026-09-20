package eu.eurostat.ui.component

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.eurostat.ui.theme.Euro

/** Smallest font size [StatTile] will shrink a fitted value to; below this the value is clipped. */
private val MIN_FIT_FONT_SIZE = 14.sp

/** Multiplier applied to the value font size on each shrink step of [FitSingleLineText]. */
private const val FIT_SHRINK_FACTOR = 0.9f

/**
 * Small statistic tile composed of a label, a tabular value and an optional
 * delta string.
 *
 * @param label the field name (e.g. `"Δ vs prev year"`).
 * @param value the tabular value (e.g. `"+1.2%"`).
 * @param delta optional secondary line (e.g. `"vs 2023"`); omitted when blank.
 * @param bordered when true, draws a 1dp translucent border around the tile.
 * @param fitValueToWidth when true, [value] is kept on a single line (never
 *   wrapping between e.g. a magnitude and its unit) and its font size is
 *   shrunk step by step, down to 14sp, until it fits the tile width. Use it
 *   in narrow multi-tile rows; the default `false` keeps the fixed-size,
 *   wrapping behaviour.
 */
@Composable
fun StatTile(
    label: String,
    value: String,
    delta: String = "",
    bordered: Boolean = false,
    modifier: Modifier = Modifier,
    fitValueToWidth: Boolean = false,
) {
    val border = if (bordered) {
        Modifier.border(1.dp, Euro.colors.border, Euro.shapes.medium)
    } else {
        Modifier
    }
    Column(
        modifier = modifier
            .then(border)
            .padding(Euro.spacing.m),
    ) {
        Text(
            text = label.uppercase(),
            style = Euro.typography.labelSmall,
            color = Euro.colors.muted,
        )
        if (fitValueToWidth) {
            FitSingleLineText(
                text = value,
                style = Euro.typography.tabularNumLarge,
                color = Euro.colors.ink,
            )
        } else {
            Text(
                text = value,
                style = Euro.typography.tabularNumLarge,
                color = Euro.colors.ink,
            )
        }
        if (delta.isNotBlank()) {
            Text(
                text = delta,
                style = Euro.typography.bodySmall,
                color = Euro.colors.muted,
            )
        }
    }
}

/**
 * Single-line [Text] that starts at `style.fontSize` and shrinks by
 * [FIT_SHRINK_FACTOR] per layout pass until it no longer overflows the
 * available width (or reaches [MIN_FIT_FONT_SIZE]). Hidden until the first fit
 * completes so the un-fitted size never flashes. When the available width
 * changes (window resize / rotation) the size is reset and re-fitted.
 */
@Composable
private fun FitSingleLineText(
    text: String,
    style: TextStyle,
    color: Color,
) {
    val baseSize: TextUnit = style.fontSize
    var fontSize by remember(text, baseSize) { mutableStateOf(baseSize) }
    var fitted by remember(text, baseSize) { mutableStateOf(false) }
    var lastWidth by remember { mutableIntStateOf(0) }
    Text(
        text = text,
        style = style.copy(fontSize = fontSize),
        color = color,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
        onTextLayout = { result ->
            if (result.didOverflowWidth && fontSize.value > MIN_FIT_FONT_SIZE.value) {
                fontSize = (fontSize.value * FIT_SHRINK_FACTOR)
                    .coerceAtLeast(MIN_FIT_FONT_SIZE.value).sp
            } else {
                fitted = true
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged { size ->
                if (lastWidth != 0 && lastWidth != size.width && fontSize != baseSize) {
                    fontSize = baseSize
                }
                lastWidth = size.width
            }
            .drawWithContent { if (fitted) drawContent() },
    )
}
