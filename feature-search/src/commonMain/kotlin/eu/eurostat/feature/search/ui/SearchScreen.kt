package eu.eurostat.feature.search.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.eurostat.feature.search.domain.IndicatorEntry
import eu.eurostat.feature.search.domain.SearchModule
import eu.eurostat.feature.search.domain.SearchSection
import eu.eurostat.ui.component.states.EmptyState
import eu.eurostat.ui.theme.Euro

/**
 * Search screen: a text field over the static indicator index.
 *
 * Empty query → browse list of every indicator grouped by module; non-blank
 * query → flat ranked results; no matches → [EmptyState]. Each result row
 * shows the module accent chip, the indicator label, the Eurostat dataset
 * code in monospace and a one-line description. Tapping a row dispatches
 * [SearchIntent.OpenResult], which navigates to the owning feature module.
 */
@Composable
fun SearchScreen(
    component: SearchComponent,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val state by component.state.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Euro.colors.paper),
    ) {
        when (val s = state) {
            is SearchUiState.Content -> {
                SearchBar(
                    query = s.query,
                    onQueryChange = { component.onIntent(SearchIntent.SetQuery(it)) },
                    onBack = onBack,
                )
                SearchResults(
                    content = s,
                    onOpen = { entry -> component.onIntent(SearchIntent.OpenResult(entry)) },
                )
            }
        }
    }
}

/**
 * Top bar with a back pill and an auto-focused query field on a warm
 * `surface2` pill. Built locally (not in core-ui): [eu.eurostat.ui.component.ModuleAppBar]
 * carries a display title, not an editable field.
 */
@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Euro.colors.paper)
            .statusBarsPadding()
            .padding(horizontal = Euro.spacing.m, vertical = Euro.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconPill(
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            description = "Back",
            onClick = onBack,
        )
        Spacer(Modifier.width(Euro.spacing.s))
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(Euro.shapes.medium)
                .background(Euro.colors.surface2)
                .padding(horizontal = Euro.spacing.m, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        text = "Search indicators",
                        style = Euro.typography.bodyLarge,
                        color = Euro.colors.muted,
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = Euro.typography.bodyLarge.copy(color = Euro.colors.ink),
                    cursorBrush = SolidColor(Euro.colors.accent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                )
            }
            if (query.isNotEmpty()) {
                Spacer(Modifier.width(Euro.spacing.s))
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Clear query",
                    tint = Euro.colors.muted,
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .clickable { onQueryChange("") },
                )
            }
        }
    }
}

/** Dispatch between browse (blank query), ranked results and no-match empty state. */
@Composable
private fun SearchResults(
    content: SearchUiState.Content,
    onOpen: (IndicatorEntry) -> Unit,
) {
    when {
        content.query.isBlank() -> BrowseList(
            sections = content.browseSections,
            onOpen = onOpen,
        )
        content.results.isEmpty() -> EmptyState(
            headline = "No matches",
            body = "No indicator matches “${content.query.trim()}”. " +
                "Try a broader term like “inflation” or “emissions”.",
        )
        else -> ResultsList(
            results = content.results,
            onOpen = onOpen,
        )
    }
}

/** All indicators grouped by module — shown while the query is blank. */
@Composable
private fun BrowseList(
    sections: List<SearchSection>,
    onOpen: (IndicatorEntry) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        sections.forEach { section ->
            item(key = "header-${section.module.name}") {
                SectionHeader(module = section.module)
            }
            section.entries.forEach { entry ->
                item(key = entry.id) {
                    IndicatorRow(entry = entry, onClick = { onOpen(entry) })
                    RowDivider()
                }
            }
        }
    }
}

/** Flat ranked matches — shown while the query is non-blank. */
@Composable
private fun ResultsList(
    results: List<IndicatorEntry>,
    onOpen: (IndicatorEntry) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        results.forEach { entry ->
            item(key = entry.id) {
                IndicatorRow(entry = entry, onClick = { onOpen(entry) })
                RowDivider()
            }
        }
    }
}

/** Uppercase module eyebrow with the module accent dot. */
@Composable
private fun SectionHeader(module: SearchModule) {
    val accent = Euro.moduleAccents.forModule(module.accentKey)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = Euro.spacing.base,
                end = Euro.spacing.base,
                top = Euro.spacing.base,
                bottom = Euro.spacing.xs,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Euro.spacing.s),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(accent, CircleShape),
        )
        Text(
            text = module.displayName.uppercase(),
            style = Euro.typography.eyebrow,
            color = accent,
        )
    }
}

/**
 * One indicator: module chip and dataset code on the meta line, then the
 * label and a one-line description.
 */
@Composable
private fun IndicatorRow(entry: IndicatorEntry, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Euro.spacing.base, vertical = Euro.spacing.m),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ModuleChip(module = entry.module)
            Spacer(Modifier.weight(1f))
            Text(
                text = entry.datasetCode,
                style = Euro.typography.tabularNumSmall,
                color = Euro.colors.muted,
            )
        }
        Spacer(Modifier.height(Euro.spacing.xs))
        Text(
            text = entry.label,
            style = Euro.typography.headlineSmall,
            color = Euro.colors.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(Euro.spacing.xxs))
        Text(
            text = entry.description,
            style = Euro.typography.bodySmall,
            color = Euro.colors.muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Small accent-tinted pill naming the owning module. */
@Composable
private fun ModuleChip(module: SearchModule) {
    val accent = Euro.moduleAccents.forModule(module.accentKey)
    Text(
        text = module.displayName.uppercase(),
        style = Euro.typography.labelSmall,
        color = accent,
        modifier = Modifier
            .clip(Euro.shapes.small)
            .background(accent.copy(alpha = 0.12f))
            .padding(horizontal = Euro.spacing.s, vertical = Euro.spacing.xxs),
    )
}

@Composable
private fun RowDivider() {
    HorizontalDivider(color = Euro.colors.muted.copy(alpha = 0.2f))
}

/**
 * Circular 36dp icon button in a 48dp tap target — the same idiom as
 * core-ui's ModuleAppBar pill (private there, so re-declared locally).
 */
@Composable
private fun IconPill(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Euro.colors.surface2, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = description,
                tint = Euro.colors.ink,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
