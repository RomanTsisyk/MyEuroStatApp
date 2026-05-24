package eu.eurostat.app.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.eurostat.core.navigation.ChildConfig
import eu.eurostat.ui.theme.Euro
import eu.eurostat.ui.theme.EuroWindowWidth
import eu.eurostat.ui.theme.LocalEuroWindowWidth

/** Metadata for each module card shown on the home grid. */
private data class ModuleEntry(
    val config: ChildConfig,
    val emoji: String,
    val title: String,
    val tagline: String,
    val accentKey: String,
)

private val MODULES = listOf(
    ModuleEntry(ChildConfig.Population,  "👥", "Population",  "Demographics & age pyramids", "population"),
    ModuleEntry(ChildConfig.Economy,     "💶", "Economy",     "GDP, inflation, deficit",      "economy"),
    ModuleEntry(ChildConfig.Environment, "🌍", "Environment", "Emissions, energy, climate",   "environment"),
    ModuleEntry(ChildConfig.Trade,       "📦", "Trade",       "Imports, exports, balance",    "trade"),
    ModuleEntry(ChildConfig.Transport,   "🚆", "Transport",   "Road, rail, air",              "transport"),
    ModuleEntry(ChildConfig.Tourism,     "🏨", "Tourism",     "Nights spent, seasonality",    "tourism"),
    ModuleEntry(ChildConfig.Social,      "🤝", "Social",      "Poverty, health, inclusion",   "social"),
    ModuleEntry(ChildConfig.Science,     "🔬", "Science",     "R&D, internet, education",     "science"),
)

/**
 * Home screen — displays a responsive grid of 8 module cards.
 * Grid columns: Compact = 2, Medium = 3, Expanded = 4.
 * Tapping a card calls [onModuleSelected] with the target [ChildConfig].
 */
@Composable
fun HomeScreen(
    onModuleSelected: (ChildConfig) -> Unit,
    modifier: Modifier = Modifier,
) {
    val windowWidth = LocalEuroWindowWidth.current
    val columns = when (windowWidth) {
        EuroWindowWidth.Compact  -> 2
        EuroWindowWidth.Medium   -> 3
        EuroWindowWidth.Expanded -> 4
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Euro.colors.paper),
    ) {
        // Header bar — dark "EU Stats" brand bar at top of home.
        // statusBarsPadding() draws the ink background ALL THE WAY to the top
        // of the device (under the status bar icons) so edge-to-edge looks
        // intentional, while pushing the actual text content down below the
        // status bar so "10:05" doesn't overlap "EU Stats".
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Euro.colors.ink)
                .statusBarsPadding()
                .padding(horizontal = Euro.spacing.base, vertical = Euro.spacing.s),
        ) {
            Text(
                text = "EU Stats",
                color = Euro.colors.paper,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
            )
            Text(
                text = "European Statistics · open data",
                color = Euro.colors.paper.copy(alpha = 0.65f),
                fontSize = 13.sp,
                letterSpacing = 0.25.sp,
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = Modifier
                .fillMaxSize()
                .padding(Euro.spacing.s),
            contentPadding = PaddingValues(Euro.spacing.s),
            verticalArrangement = Arrangement.spacedBy(Euro.spacing.s),
            horizontalArrangement = Arrangement.spacedBy(Euro.spacing.s),
        ) {
            items(MODULES) { entry ->
                ModuleCard(
                    entry = entry,
                    onClick = { onModuleSelected(entry.config) },
                )
            }
        }
    }
}

@Composable
private fun ModuleCard(
    entry: ModuleEntry,
    onClick: () -> Unit,
) {
    val accent: Color = Euro.moduleAccents.forModule(entry.accentKey)
    val shape = Euro.shapes.medium

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(accent.copy(alpha = 0.12f), shape)
            .clickable(onClick = onClick)
            .padding(Euro.spacing.base),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = entry.emoji,
            fontSize = 32.sp,
        )
        Text(
            text = entry.title,
            color = Euro.colors.ink,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 20.sp,
        )
        Text(
            text = entry.tagline,
            color = Euro.colors.ink.copy(alpha = 0.60f),
            fontSize = 12.sp,
            lineHeight = 16.sp,
        )
    }
}
