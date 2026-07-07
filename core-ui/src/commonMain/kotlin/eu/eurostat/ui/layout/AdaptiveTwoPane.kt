package eu.eurostat.ui.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import eu.eurostat.ui.theme.Euro
import eu.eurostat.ui.theme.EuroWindowWidth
import eu.eurostat.ui.theme.LocalEuroWindowWidth

/** Default width of the fixed controls pane in the expanded (≥ 840 dp) layout. */
val ControlsPaneWidth: Dp = 320.dp

/**
 * Responsive master-detail wrapper shared by all feature screens.
 *
 * Reads [LocalEuroWindowWidth] (provided by [AdaptiveScaffold] at the app root)
 * and switches between two structures:
 *
 * - **Compact / Medium (< 840 dp):** a single scrollable [Column], top-centered
 *   and capped at [adaptiveContentMaxWidth], rendering the [compact] slot — the
 *   screen's existing phone-first ordering, unchanged. When [compact] is null,
 *   [controls] followed by [content] is rendered instead.
 * - **Expanded (≥ 840 dp):** a two-pane [Row] — a fixed-width (default 320 dp)
 *   independently scrollable left [Column] hosting [controls] (country chips,
 *   year scrubbers/dropdowns, metric switchers) and a `weight(1f)` right
 *   [Column] hosting [content] (hero chart, KPI tiles, secondary content).
 *   The whole row is capped at `controlsPaneWidth + adaptiveContentMaxWidth()`
 *   and centered so very wide desktop windows don't produce absurd line lengths.
 *
 * Both panes scroll vertically and independently, so window resizes transition
 * smoothly with no layout snap-back. The wrapper owns only structure: state,
 * intents and pull-to-refresh wiring stay entirely with the caller.
 *
 * @param controls Slot with the screen's interactive controls, laid out top-to-
 *   bottom in the left pane when expanded. Only composed at ≥ 840 dp (or below
 *   840 dp when [compact] is null).
 * @param content Slot with the screen's primary content (hero chart, tiles).
 *   Right pane when expanded; below [controls] when compact without a [compact]
 *   slot.
 * @param modifier Applied to the outermost [Box]. Callers embedding the wrapper
 *   next to a footer should pass their own size/weight modifier
 *   (e.g. `Modifier.weight(1f)`); it defaults to filling nothing.
 * @param compact Optional slot rendering the exact phone-first section ordering
 *   (< 840 dp). Screens whose controls interleave with content should provide
 *   this so the phone layout stays pixel-identical.
 * @param controlsPaneWidth Width of the fixed left pane in expanded mode.
 * @param sectionSpacing Vertical gap between sections in every pane.
 * @param contentPadding Padding applied inside each scrollable pane.
 */
@Composable
fun AdaptiveTwoPane(
    controls: @Composable ColumnScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    compact: (@Composable ColumnScope.() -> Unit)? = null,
    controlsPaneWidth: Dp = ControlsPaneWidth,
    sectionSpacing: Dp = Euro.spacing.m,
    contentPadding: PaddingValues = PaddingValues(horizontal = Euro.spacing.base),
) {
    val isExpanded = LocalEuroWindowWidth.current == EuroWindowWidth.Expanded
    Box(
        modifier = modifier,
        contentAlignment = Alignment.TopCenter,
    ) {
        if (isExpanded) {
            val maxRowWidth = controlsPaneWidth + adaptiveContentMaxWidth()
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = maxRowWidth),
            ) {
                Column(
                    modifier = Modifier
                        .width(controlsPaneWidth)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(contentPadding),
                    verticalArrangement = Arrangement.spacedBy(sectionSpacing),
                    content = controls,
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(contentPadding),
                    verticalArrangement = Arrangement.spacedBy(sectionSpacing),
                    content = content,
                )
            }
        } else {
            val maxW = adaptiveContentMaxWidth()
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .then(if (maxW != Dp.Unspecified) Modifier.widthIn(max = maxW) else Modifier)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(contentPadding),
                verticalArrangement = Arrangement.spacedBy(sectionSpacing),
                content = compact ?: {
                    controls()
                    content()
                },
            )
        }
    }
}
