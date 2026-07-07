package eu.eurostat.feature.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import eu.eurostat.core.common.EurostatCountries
import eu.eurostat.core.common.prefs.AppPreferences
import eu.eurostat.core.common.prefs.ThemePreference
import eu.eurostat.ui.component.CountryPickerSheet
import eu.eurostat.ui.component.MetricDropdown
import eu.eurostat.ui.component.ModuleAppBar
import eu.eurostat.ui.component.SegmentedControl
import eu.eurostat.ui.component.states.LoadingShimmer
import eu.eurostat.ui.theme.Euro

/**
 * Settings screen: theme, language, default country, cache maintenance and
 * an About row. All values are persisted through [SettingsComponent] and
 * survive app restarts.
 */
@Composable
fun SettingsScreen(
    component: SettingsComponent,
    onBack: () -> Unit = {},
) {
    val state by component.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Euro.colors.paper),
    ) {
        ModuleAppBar(
            title = "Settings",
            accent = Euro.colors.accent,
            onBack = onBack,
        )
        when (val s = state) {
            is SettingsUiState.Loading -> LoadingShimmer(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Euro.spacing.base),
            )
            is SettingsUiState.Content -> SettingsContent(
                content = s,
                onIntent = component::onIntent,
            )
        }
    }
}

@Composable
private fun SettingsContent(
    content: SettingsUiState.Content,
    onIntent: (SettingsIntent) -> Unit,
) {
    var showCountryPicker by remember { mutableStateOf(false) }

    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        item {
            ThemeRow(
                selected = content.themePreference,
                onSelect = { onIntent(SettingsIntent.SetTheme(it)) },
            )
        }
        item { SettingsDivider() }
        item {
            LanguageRow(
                languageCode = content.language,
                onSelect = { onIntent(SettingsIntent.SetLanguage(it)) },
            )
        }
        item { SettingsDivider() }
        item {
            SettingsListItem(
                headline = "Default country",
                supporting = countryLabel(content.defaultCountry),
                onClick = { showCountryPicker = true },
            )
        }
        item { SettingsDivider() }
        item {
            SettingsListItem(
                headline = "Clear cache",
                supporting = when {
                    content.isClearingCache -> "Clearing…"
                    content.cacheCleared -> "Cache cleared"
                    else -> "Remove all downloaded statistics"
                },
                onClick = if (content.isClearingCache) {
                    null
                } else {
                    { onIntent(SettingsIntent.ClearCache) }
                },
            )
        }
        item { SettingsDivider() }
        item {
            SettingsListItem(
                headline = "About",
                supporting = "v${content.appVersion} · independent Eurostat API client",
            )
        }
    }

    if (showCountryPicker) {
        CountryPickerSheet(
            selected = setOf(content.defaultCountry),
            onConfirm = { codes ->
                // Single-select usage of the multi-select sheet: prefer the
                // newly ticked code; keep the current one if nothing changed.
                val newCode = (codes - content.defaultCountry).firstOrNull()
                    ?: codes.firstOrNull()
                if (newCode != null && newCode != content.defaultCountry) {
                    onIntent(SettingsIntent.SetDefaultCountry(newCode))
                }
                showCountryPicker = false
            },
            onDismiss = { showCountryPicker = false },
            title = "Default country",
            maxSelection = 2,
        )
    }
}

/** Theme mode selector: label above a three-way [SegmentedControl]. */
@Composable
private fun ThemeRow(
    selected: ThemePreference,
    onSelect: (ThemePreference) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Euro.spacing.base, vertical = Euro.spacing.s),
    ) {
        Text(
            text = "Theme",
            style = Euro.typography.labelLarge,
            color = Euro.colors.ink,
        )
        Spacer(Modifier.height(Euro.spacing.s))
        SegmentedControl(
            options = ThemePreference.entries.map { it.displayLabel() },
            selectedIndex = selected.ordinal,
            onSelect = { index -> onSelect(ThemePreference.entries[index]) },
        )
    }
}

/** Language selector using the pill-dropdown idiom from the chart screens. */
@Composable
private fun LanguageRow(
    languageCode: String,
    onSelect: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Euro.spacing.base, vertical = Euro.spacing.s),
    ) {
        MetricDropdown(
            label = "Language",
            value = languageLabel(languageCode),
            options = AppPreferences.SUPPORTED_LANGUAGES.map { languageLabel(it) },
            onSelect = { label -> onSelect(languageCodeFor(label)) },
        )
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(color = Euro.colors.muted.copy(alpha = 0.2f))
}

@Composable
private fun SettingsListItem(
    headline: String,
    supporting: String? = null,
    onClick: (() -> Unit)? = null,
) {
    ListItem(
        headlineContent = {
            Text(
                text = headline,
                style = Euro.typography.labelLarge,
                color = Euro.colors.ink,
            )
        },
        supportingContent = if (supporting != null) {
            {
                Text(
                    text = supporting,
                    style = Euro.typography.bodySmall,
                    color = Euro.colors.muted,
                )
            }
        } else {
            null
        },
        colors = ListItemDefaults.colors(containerColor = Euro.colors.paper),
        modifier = Modifier
            .fillMaxWidth()
            .let { if (onClick != null) it.clickable(onClick = onClick) else it },
    )
}

/** UI label for a [ThemePreference] segment. */
private fun ThemePreference.displayLabel(): String = when (this) {
    ThemePreference.SYSTEM -> "System"
    ThemePreference.LIGHT -> "Light"
    ThemePreference.DARK -> "Dark"
}

/** Display name for a persisted language code. */
private fun languageLabel(code: String): String = when (code) {
    "en" -> "English"
    "pl" -> "Polski"
    "uk" -> "Українська"
    else -> "System"
}

/** Inverse of [languageLabel]; unknown labels fall back to `"system"`. */
private fun languageCodeFor(label: String): String = when (label) {
    "English" -> "en"
    "Polski" -> "pl"
    "Українська" -> "uk"
    else -> "system"
}

/** Flag + name + code line for the default-country row. */
private fun countryLabel(code: String): String {
    val country = EurostatCountries.byCode(code) ?: return code
    return "${country.flag} ${country.name} · ${country.code}"
}
