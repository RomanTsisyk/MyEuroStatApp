package eu.eurostat.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.Euro
import androidx.compose.material.icons.outlined.FlightTakeoff
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Park
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import eu.eurostat.ui.theme.Euro
import eu.eurostat.ui.theme.EuroPlatform
import kotlinx.coroutines.launch

/**
 * All navigable module destinations plus the synthetic [More] entry that
 * triggers the overflow bottom sheet.
 */
enum class BottomTabDestination {
    Population,
    Economy,
    Environment,
    Trade,
    // overflow group — shown in the More sheet
    Transport,
    Tourism,
    Social,
    Science,
    // synthetic: tapping this opens the overflow sheet
    More,
    // utility destination — visible only in the More sheet and adaptive nav
    Settings,
}

/** The 4 primary items always visible in the [NavigationBar], plus [BottomTabDestination.More]. */
private val visibleDestinations = listOf(
    BottomTabDestination.Population,
    BottomTabDestination.Economy,
    BottomTabDestination.Environment,
    BottomTabDestination.More,
)

/** The 5 module items hidden behind the More sheet — Trade through Science. */
internal val overflowDestinations = listOf(
    BottomTabDestination.Trade,
    BottomTabDestination.Transport,
    BottomTabDestination.Tourism,
    BottomTabDestination.Social,
    BottomTabDestination.Science,
)

/** Settings destination shown at the bottom of the More sheet. */
internal val overflowSettingsDestination = BottomTabDestination.Settings

/**
 * Hi-fi M3 Expressive bottom navigation. Renders four primary destinations
 * plus a "More" overflow that opens a [ModalBottomSheet] with Trade,
 * Transport, Tourism, Social, Science.
 *
 * Each item shows an icon inside an accent-tinted pill indicator (visible only
 * when active), with an Inter label underneath. The bar sits on a `paper`
 * background with a hairline top border instead of the default Material
 * surface tint.
 *
 * @param selected the currently active module destination.
 * @param onSelect invoked when the user picks a destination (never [BottomTabDestination.More]).
 * @param accent per-active-module accent used for the indicator pill.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomTabBar(
    selected: BottomTabDestination,
    onSelect: (BottomTabDestination) -> Unit,
    modifier: Modifier = Modifier,
    accent: androidx.compose.ui.graphics.Color = Euro.colors.accent,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var showSheet by remember { mutableStateOf(false) }

    // The "More" icon is highlighted when the active module is in the overflow group.
    val moreSelected = selected in overflowDestinations

    if (Euro.platform == EuroPlatform.Ios) {
        IosTabBar(
            modifier = modifier,
            selected = selected,
            moreSelected = moreSelected,
            accent = accent,
            onShowSheet = { showSheet = true },
            onSelect = onSelect,
        )
    } else {
        AndroidTabBar(
            modifier = modifier,
            selected = selected,
            moreSelected = moreSelected,
            accent = accent,
            onShowSheet = { showSheet = true },
            onSelect = onSelect,
        )
    }

    if (showSheet) {
        MoreSheet(
            sheetState = sheetState,
            scope = scope,
            onDismiss = { showSheet = false },
            onSelect = onSelect,
        )
    }
}

/**
 * Android M3 Expressive tab bar — solid `paper` background with a hairline
 * top border and pill indicator on the active item.
 */
@Composable
private fun AndroidTabBar(
    modifier: Modifier,
    selected: BottomTabDestination,
    moreSelected: Boolean,
    accent: androidx.compose.ui.graphics.Color,
    onShowSheet: () -> Unit,
    onSelect: (BottomTabDestination) -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Euro.colors.paper)
            .border(width = 1.dp, color = Euro.colors.border)
            .navigationBarsPadding()
            .padding(start = Euro.spacing.s, end = Euro.spacing.s, top = 6.dp, bottom = 14.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        visibleDestinations.forEach { dest ->
            val isSelected = if (dest == BottomTabDestination.More) moreSelected else dest == selected
            NavItem(
                icon = iconFor(dest),
                label = labelFor(dest),
                selected = isSelected,
                accent = accent,
                onClick = {
                    if (dest == BottomTabDestination.More) onShowSheet() else onSelect(dest)
                },
            )
        }
    }
}

/**
 * iOS floating capsule tab bar — glass background, 0.5dp glassBorder outline,
 * subtle shadow, and 12dp horizontal + 24dp bottom padding so it floats above
 * the home indicator area.
 *
 * True backdrop blur is not available in commonMain without a third-party
 * library; the semi-transparent [Euro.colors.glass] token approximates the
 * frosted appearance.
 */
@Composable
private fun IosTabBar(
    modifier: Modifier,
    selected: BottomTabDestination,
    moreSelected: Boolean,
    accent: androidx.compose.ui.graphics.Color,
    onShowSheet: () -> Unit,
    onSelect: (BottomTabDestination) -> Unit,
) {
    val capsuleShape = RoundedCornerShape(percent = 50)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, bottom = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .shadow(elevation = 8.dp, shape = capsuleShape)
                .clip(capsuleShape)
                .background(Euro.colors.glass)
                .border(width = 0.5.dp, color = Euro.colors.glassBorder, shape = capsuleShape)
                .padding(start = Euro.spacing.s, end = Euro.spacing.s, top = 6.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            visibleDestinations.forEach { dest ->
                val isSelected = if (dest == BottomTabDestination.More) moreSelected else dest == selected
                NavItem(
                    icon = iconFor(dest),
                    label = labelFor(dest),
                    selected = isSelected,
                    accent = accent,
                    onClick = {
                        if (dest == BottomTabDestination.More) onShowSheet() else onSelect(dest)
                    },
                )
            }
        }
    }
}

/** Shared overflow bottom sheet used by both platform variants. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoreSheet(
    sheetState: androidx.compose.material3.SheetState,
    scope: kotlinx.coroutines.CoroutineScope,
    onDismiss: () -> Unit,
    onSelect: (BottomTabDestination) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Euro.colors.paper,
        contentColor = Euro.colors.ink,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
        ) {
            items(overflowDestinations) { dest ->
                ListItem(
                    headlineContent = {
                        Text(labelFor(dest), style = Euro.typography.labelLarge)
                    },
                    leadingContent = {
                        Icon(iconFor(dest), contentDescription = labelFor(dest))
                    },
                    modifier = Modifier.clickable {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            onDismiss()
                        }
                        onSelect(dest)
                    },
                )
            }
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            }
            item {
                val dest = overflowSettingsDestination
                ListItem(
                    headlineContent = {
                        Text(labelFor(dest), style = Euro.typography.labelLarge)
                    },
                    leadingContent = {
                        Icon(iconFor(dest), contentDescription = labelFor(dest))
                    },
                    modifier = Modifier.clickable {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            onDismiss()
                        }
                        onSelect(dest)
                    },
                )
            }
        }
    }
}

/**
 * Single tab — icon inside an accent-tinted pill (when active) with a label
 * underneath. The pill background is the accent color at ~15% alpha; the icon
 * and label switch to the accent / ink color when selected.
 */
@Composable
private fun NavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    accent: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .clip(RoundedCornerShape(percent = 50))
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .height(28.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(if (selected) accent.copy(alpha = 0.15f) else androidx.compose.ui.graphics.Color.Transparent)
                .padding(horizontal = 18.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) accent else Euro.colors.muted,
                modifier = Modifier.size(20.dp),
            )
        }
        Text(
            text = label,
            style = Euro.typography.labelSmall,
            color = if (selected) Euro.colors.ink else Euro.colors.muted,
        )
    }
}

internal fun labelFor(destination: BottomTabDestination): String = when (destination) {
    BottomTabDestination.Population -> "Population"
    BottomTabDestination.Economy -> "Economy"
    BottomTabDestination.Environment -> "Climate"
    BottomTabDestination.Trade -> "Trade"
    BottomTabDestination.Transport -> "Transport"
    BottomTabDestination.Tourism -> "Tourism"
    BottomTabDestination.Social -> "Social"
    BottomTabDestination.Science -> "Science"
    BottomTabDestination.More -> "More"
    BottomTabDestination.Settings -> "Settings"
}

internal fun iconFor(destination: BottomTabDestination): ImageVector = when (destination) {
    BottomTabDestination.Population -> Icons.Outlined.People
    BottomTabDestination.Economy -> Icons.Outlined.Euro
    BottomTabDestination.Environment -> Icons.Outlined.Park
    BottomTabDestination.Trade -> Icons.Outlined.ShoppingCart
    BottomTabDestination.Transport -> Icons.Outlined.DirectionsBus
    BottomTabDestination.Tourism -> Icons.Outlined.FlightTakeoff
    BottomTabDestination.Social -> Icons.Outlined.Groups
    BottomTabDestination.Science -> Icons.Outlined.Science
    BottomTabDestination.More -> Icons.Outlined.MoreHoriz
    BottomTabDestination.Settings -> Icons.Outlined.Settings
}
