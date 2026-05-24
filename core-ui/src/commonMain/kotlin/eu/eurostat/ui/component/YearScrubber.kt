package eu.eurostat.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.eurostat.ui.theme.Euro
import kotlin.math.roundToInt

/**
 * Year range slider with tabular year labels rendered above the track.
 *
 * @param min lower bound of the available year range, inclusive.
 * @param max upper bound of the available year range, inclusive.
 * @param value current selected start..end pair (inclusive). Values are
 *   clamped and rounded to whole years.
 * @param onValueChange invoked with the new whole-year range as the user
 *   drags either thumb.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YearScrubber(
    min: Int,
    max: Int,
    value: IntRange,
    onValueChange: (IntRange) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = value.first.toString(),
                style = Euro.typography.tabularNumSmall,
                color = Euro.colors.ink,
            )
            Text(
                text = value.last.toString(),
                style = Euro.typography.tabularNumSmall,
                color = Euro.colors.ink,
            )
        }
        RangeSlider(
            value = value.first.toFloat()..value.last.toFloat(),
            onValueChange = { range ->
                val lo = range.start.roundToInt().coerceIn(min, max)
                val hi = range.endInclusive.roundToInt().coerceIn(min, max)
                onValueChange(lo..hi)
            },
            valueRange = min.toFloat()..max.toFloat(),
            steps = (max - min - 1).coerceAtLeast(0),
            colors = SliderDefaults.colors(
                thumbColor = Euro.colors.ink,
                activeTrackColor = Euro.colors.accent,
                inactiveTrackColor = Euro.colors.mutedAlt,
            ),
        )
    }
}
