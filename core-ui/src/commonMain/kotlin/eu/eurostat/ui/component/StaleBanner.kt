package eu.eurostat.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import eu.eurostat.ui.theme.Euro

/**
 * Soft warn-tinted banner shown when cached data is displayed while a refresh
 * is in flight. Matches the hi-fi spec: pale sienna fill, hairline sienna
 * border, dot indicator, sienna body text.
 */
@Composable
fun StaleBanner(
    modifier: Modifier = Modifier,
    message: String = "showing cached data · refreshing…",
) {
    val shape: RoundedCornerShape = RoundedCornerShape(10.dp)
    val warn = Euro.colors.warn
    val isDark = Euro.isDark
    val bg = if (isDark) warn.copy(alpha = 0.15f) else warn.copy(alpha = 0.10f)
    val borderColor = if (isDark) warn.copy(alpha = 0.30f) else warn.copy(alpha = 0.25f)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(bg, shape)
            .border(1.dp, borderColor, shape)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(
            modifier = Modifier
                .size(6.dp)
                .background(warn, CircleShape),
        )
        Spacer(Modifier.width(Euro.spacing.s))
        Text(
            text = message,
            style = Euro.typography.bodySmall,
            color = warn,
        )
    }
}
