package eu.eurostat.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import eu.eurostat.ui.theme.Euro

/**
 * Hi-fi M3 Expressive segmented button group.
 *
 * Behaves like a connected row of buttons: first and last segments have fully
 * rounded outer corners (pill caps), middle segments have small 6dp corners.
 * The active segment is filled with [activeColor] and prefixed with a check
 * icon (M3 Expressive selection cue); inactive segments are outlined.
 *
 * @param options textual options shown left-to-right.
 * @param selectedIndex index of the active segment.
 * @param onSelect invoked with the new index when the user taps a segment.
 * @param activeColor optional override for the active fill color (e.g. the module accent).
 */
@Composable
fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = Euro.colors.accent,
) {
    val pillCap = CornerSize(percent = 50)
    val midCap = CornerSize(6.dp)
    val pill = RoundedCornerShape(pillCap, pillCap, pillCap, pillCap)
    val mid = RoundedCornerShape(midCap, midCap, midCap, midCap)
    val leftCap = RoundedCornerShape(pillCap, midCap, midCap, pillCap)
    val rightCap = RoundedCornerShape(midCap, pillCap, pillCap, midCap)
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEachIndexed { idx, label ->
            val selected = idx == selectedIndex
            val isFirst = idx == 0
            val isLast = idx == options.lastIndex
            val shape = when {
                isFirst && isLast -> pill
                isFirst -> leftCap
                isLast -> rightCap
                else -> mid
            }
            val bg = if (selected) activeColor else Color.Transparent
            val fg = if (selected) Color.White else Euro.colors.ink
            val borderColor = if (selected) activeColor else Euro.colors.border
            Row(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .clip(shape)
                    .background(bg, shape)
                    .border(1.dp, borderColor, shape)
                    .clickable { onSelect(idx) }
                    .padding(vertical = Euro.spacing.s, horizontal = Euro.spacing.s),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                if (selected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(Euro.spacing.xs))
                }
                Text(
                    text = label,
                    style = Euro.typography.labelLarge,
                    color = fg,
                )
            }
        }
    }
}
