package eu.eurostat.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Three canonical width buckets — phone, tablet, desktop/web. */
enum class EuroWindowWidth { Compact, Medium, Expanded }

/**
 * Breakpoint thresholds that match Material 3 / Compose adaptive recommendations:
 * - Compact  < 600 dp  (phone portrait)
 * - Medium   < 840 dp  (phone landscape, small tablet)
 * - Expanded ≥ 840 dp  (large tablet, desktop, web)
 */
object EuroBreakpoints {
    val medium: Dp = 600.dp
    val expanded: Dp = 840.dp
}

/**
 * CompositionLocal that carries the current [EuroWindowWidth] down the tree.
 * Provided by [eu.eurostat.ui.layout.AdaptiveScaffold].
 * Defaults to [EuroWindowWidth.Compact] so previews and tests work without a provider.
 */
val LocalEuroWindowWidth = staticCompositionLocalOf { EuroWindowWidth.Compact }

/**
 * Converts a measured available width to the corresponding [EuroWindowWidth] bucket.
 *
 * @param widthDp The available width in density-independent pixels.
 */
fun classifyWidth(widthDp: Dp): EuroWindowWidth = when {
    widthDp < EuroBreakpoints.medium   -> EuroWindowWidth.Compact
    widthDp < EuroBreakpoints.expanded -> EuroWindowWidth.Medium
    else                               -> EuroWindowWidth.Expanded
}
