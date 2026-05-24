package eu.eurostat.ui.layout

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import eu.eurostat.ui.theme.EuroWindowWidth
import eu.eurostat.ui.theme.LocalEuroWindowWidth
import eu.eurostat.ui.theme.classifyWidth

/**
 * Root layout wrapper that measures the available width with [BoxWithConstraints],
 * classifies it into a [EuroWindowWidth] bucket, and provides the result via
 * [LocalEuroWindowWidth] so that every descendant composable can read the current
 * breakpoint without threading parameters through the call tree.
 *
 * Usage at the app root:
 * ```kotlin
 * AdaptiveScaffold { windowWidth ->
 *     // windowWidth == LocalEuroWindowWidth.current here
 *     MyContent()
 * }
 * ```
 *
 * Feature screens that only need the value inline can simply read
 * `LocalEuroWindowWidth.current` or call [adaptiveChartHeight] / [adaptiveContentMaxWidth].
 *
 * @param modifier Optional modifier applied to the outer [BoxWithConstraints].
 * @param content  Slot that receives the resolved [EuroWindowWidth].
 */
@Composable
fun AdaptiveScaffold(
    modifier: Modifier = Modifier,
    content: @Composable (EuroWindowWidth) -> Unit,
) {
    BoxWithConstraints(modifier = modifier) {
        val windowWidth = classifyWidth(maxWidth)
        CompositionLocalProvider(LocalEuroWindowWidth provides windowWidth) {
            content(windowWidth)
        }
    }
}

// ---------------------------------------------------------------------------
// Adaptive helpers — read LocalEuroWindowWidth; call from any composable inside
// an AdaptiveScaffold (or after providing LocalEuroWindowWidth manually in tests).
// ---------------------------------------------------------------------------

/**
 * Returns an appropriate chart height for the current window-width bucket.
 *
 * Override defaults per-call site when a specific chart needs more vertical space:
 * ```kotlin
 * val pyramidHeight = adaptiveChartHeight(compact = 260.dp, medium = 340.dp, expanded = 440.dp)
 * ```
 */
@Composable
fun adaptiveChartHeight(
    compact: Dp = 200.dp,
    medium: Dp = 280.dp,
    expanded: Dp = 360.dp,
): Dp = when (LocalEuroWindowWidth.current) {
    EuroWindowWidth.Compact  -> compact
    EuroWindowWidth.Medium   -> medium
    EuroWindowWidth.Expanded -> expanded
}

/**
 * Returns the maximum content width to apply (e.g. via `widthIn(max = adaptiveContentMaxWidth())`)
 * so that line lengths and chart widths don't become unwieldy on large screens.
 *
 * Returns [Dp.Unspecified] on compact screens so content fills the full width naturally.
 */
@Composable
fun adaptiveContentMaxWidth(): Dp = when (LocalEuroWindowWidth.current) {
    EuroWindowWidth.Compact  -> Dp.Unspecified
    EuroWindowWidth.Medium   -> 720.dp
    EuroWindowWidth.Expanded -> 1040.dp
}
