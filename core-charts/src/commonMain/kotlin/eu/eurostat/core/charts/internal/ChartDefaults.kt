package eu.eurostat.core.charts.internal

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Design tokens for charts. Values are aligned with `design/sketch.css` and
 * `core-ui/EurostatColors.kt` light theme to prevent palette drift.
 *
 * Charts accept color parameters so callers can pass module accents; these
 * constants serve as defaults and for internal chrome (grid lines, axis labels).
 *
 * When dark-theme support is added, convert to `@Composable @ReadOnlyComposable`
 * accessors that read from `MaterialTheme.colorScheme`.
 */
internal object ChartDefaults {
    /** Matches `core-ui/EurostatColors.lightColors().ink = #1A1817` (≈sketch.css `#1f1d1a`). */
    val Ink: Color = Color(0xFF1A1817)
    /** Matches `core-ui/EurostatColors.lightColors().muted = #6B6359` (sketch.css `#6b6359`, 5.10:1 on paper). */
    val Muted: Color = Color(0xFF6B6359)
    /** Matches `core-ui/EurostatColors.lightColors().mutedAlt = #B3ACA1`. */
    val MutedAlt: Color = Color(0xFFB3ACA1)
    /** Matches `core-ui/EurostatColors.lightColors().paper = #F4F1EA` (sketch.css `#faf8f3`). */
    val Paper: Color = Color(0xFFF4F1EA)
    /** Matches `core-ui/EurostatColors.lightColors().accent = #2F4969` (sketch.css `#2f4969`). */
    val Accent: Color = Color(0xFF2F4969)
    /** Secondary accent: trade module color for back-compat in previews. */
    val AccentSecondary: Color = Color(0xFF7A5C46)
    /** Warn color matches `core-ui/EurostatColors.lightColors().warn`. */
    val Warn: Color = Color(0xFFC25A35)
    /** Grid line at low opacity over paper. */
    val GridLine: Color = Ink.copy(alpha = 0.08f)

    val DefaultStrokeDp = 2.dp
    val ThinStrokeDp = 1.dp
    val AxisGutterDp = 36.dp
    val AxisLabelGapDp = 4.dp

    /** Axis tick label style with tabular numerals so digits align column-wise. */
    val AxisLabelStyle: TextStyle = TextStyle(
        color = Muted,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        fontFeatureSettings = "tnum",
    )

    /** Slightly emphasized label used for category labels. */
    val CategoryLabelStyle: TextStyle = TextStyle(
        color = Ink,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
    )
}
