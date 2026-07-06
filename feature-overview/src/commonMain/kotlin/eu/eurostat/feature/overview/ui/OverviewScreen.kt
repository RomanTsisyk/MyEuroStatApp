package eu.eurostat.feature.overview.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.eurostat.core.navigation.ChildConfig
import eu.eurostat.ui.component.MetricHeadline
import eu.eurostat.ui.component.SourceFooter
import eu.eurostat.ui.theme.Euro
import eu.eurostat.ui.theme.EuroWindowWidth
import eu.eurostat.ui.theme.LocalEuroWindowWidth

/**
 * Overview dashboard — the app's landing screen.
 *
 * Renders a hero headline (Economy · GDP for the default country) followed by a
 * responsive grid of per-module teaser tiles. Tapping a tile navigates to that
 * feature via [onModuleSelected].
 */
@Composable
fun OverviewScreen(
    component: OverviewComponent,
    onModuleSelected: (ChildConfig) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by component.state.collectAsState()

    val columns = when (LocalEuroWindowWidth.current) {
        EuroWindowWidth.Compact -> 2
        EuroWindowWidth.Medium -> 3
        EuroWindowWidth.Expanded -> 4
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier
            .fillMaxSize()
            .background(Euro.colors.paper),
        contentPadding = PaddingValues(bottom = Euro.spacing.l),
        verticalArrangement = Arrangement.spacedBy(Euro.spacing.s),
        horizontalArrangement = Arrangement.spacedBy(Euro.spacing.s),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) { HeaderBar() }

        item(span = { GridItemSpan(maxLineSpan) }) {
            HeroBlock(hero = state.hero)
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                text = "BROWSE",
                color = Euro.colors.muted,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.2.sp,
                modifier = Modifier.padding(horizontal = Euro.spacing.base, vertical = Euro.spacing.xs),
            )
        }

        items(state.teasers, key = { it.accentKey }) { teaser ->
            TeaserTile(teaser = teaser, onClick = { onModuleSelected(teaser.destination) })
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            SourceFooter(
                dataset = "eurostat.europa.eu",
                staleness = "live open data",
                modifier = Modifier.padding(top = Euro.spacing.s),
            )
        }
    }
}

@Composable
private fun HeaderBar() {
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
}

@Composable
private fun HeroBlock(hero: ModuleTeaser?) {
    val accent = Euro.moduleAccents.forModule("economy")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Euro.spacing.base),
    ) {
        MetricHeadline(
            value = hero?.value ?: "—",
            unit = "B €",
            subtitle = "GDP · Germany · live",
            year = hero?.year?.toString() ?: "—",
            accent = accent,
        )
    }
}

@Composable
private fun TeaserTile(teaser: ModuleTeaser, onClick: () -> Unit) {
    val accent = Euro.moduleAccents.forModule(teaser.accentKey)
    val shape = Euro.shapes.medium

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(accent.copy(alpha = 0.12f), shape)
            .clickable(onClick = onClick)
            .padding(Euro.spacing.base),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(text = teaser.emoji, fontSize = 20.sp)
            Text(
                text = teaser.title,
                color = Euro.colors.ink,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Text(
            text = teaser.value,
            color = Euro.colors.ink,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = teaser.unit + (teaser.year?.let { " · $it" } ?: ""),
            color = Euro.colors.ink.copy(alpha = 0.60f),
            fontSize = 11.sp,
            lineHeight = 14.sp,
        )
    }
}
