package eu.eurostat.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import eu.eurostat.ui.theme.Euro

/**
 * Plain text-row tabs with a 3dp underline bar in [activeColor] under the
 * selected tab. Used for in-screen content tabs (e.g. "Trends · Compare ·
 * Rankings").
 *
 * @param tabs labels in source order.
 * @param selectedIndex active tab index.
 * @param onSelect invoked when a tab is tapped.
 * @param activeColor underline color, typically the module accent.
 */
@Composable
fun UnderlineTabs(
    tabs: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = Euro.colors.accent,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
    ) {
        tabs.forEachIndexed { idx, label ->
            val selected = idx == selectedIndex
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onSelect(idx) }
                    .defaultMinSize(minHeight = 48.dp)
                    .padding(horizontal = Euro.spacing.m, vertical = Euro.spacing.s),
            ) {
                Text(
                    text = label,
                    style = Euro.typography.labelLarge,
                    color = if (selected) Euro.colors.ink else Euro.colors.muted,
                )
                Box(
                    modifier = Modifier
                        .padding(top = Euro.spacing.xs)
                        .height(3.dp)
                        .background(if (selected) activeColor else Color.Transparent)
                        .fillMaxWidth(),
                )
            }
        }
    }
}
