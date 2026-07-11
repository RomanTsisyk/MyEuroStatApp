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
import myeurostatapp.feature_settings.generated.resources.Res
import myeurostatapp.feature_settings.generated.resources.settings_about_label
import myeurostatapp.feature_settings.generated.resources.settings_about_version
import myeurostatapp.feature_settings.generated.resources.settings_clear_cache_cleared
import myeurostatapp.feature_settings.generated.resources.settings_clear_cache_clearing
import myeurostatapp.feature_settings.generated.resources.settings_clear_cache_label
import myeurostatapp.feature_settings.generated.resources.settings_clear_cache_subtitle
import myeurostatapp.feature_settings.generated.resources.settings_default_country_label
import myeurostatapp.feature_settings.generated.resources.settings_language_english
import myeurostatapp.feature_settings.generated.resources.settings_language_label
import myeurostatapp.feature_settings.generated.resources.settings_language_polski
import myeurostatapp.feature_settings.generated.resources.settings_language_system
import myeurostatapp.feature_settings.generated.resources.settings_language_ukrainska
import myeurostatapp.feature_settings.generated.resources.settings_module_title
import myeurostatapp.feature_settings.generated.resources.settings_theme_dark
import myeurostatapp.feature_settings.generated.resources.settings_theme_label
import myeurostatapp.feature_settings.generated.resources.settings_theme_light
import myeurostatapp.feature_settings.generated.resources.settings_theme_system
import org.jetbrains.compose.resources.stringResource

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
            title = stringResource(Res.string.settings_module_title),
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

    // stringResource is @Composable — resolve every label here (some are
    // reused across two call sites, e.g. the default-country row and the
    // picker sheet title) before handing plain Strings down.
    val defaultCountryLabel = stringResource(Res.string.settings_default_country_label)
    val clearCacheLabel = stringResource(Res.string.settings_clear_cache_label)
    val clearingLabel = stringResource(Res.string.settings_clear_cache_clearing)
    val cacheClearedLabel = stringResource(Res.string.settings_clear_cache_cleared)
    val clearCacheSubtitle = stringResource(Res.string.settings_clear_cache_subtitle)
    val aboutLabel = stringResource(Res.string.settings_about_label)
    val aboutVersion = stringResource(Res.string.settings_about_version, content.appVersion)

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
                headline = defaultCountryLabel,
                supporting = countryLabel(content.defaultCountry),
                onClick = { showCountryPicker = true },
            )
        }
        item { SettingsDivider() }
        item {
            SettingsListItem(
                headline = clearCacheLabel,
                supporting = when {
                    content.isClearingCache -> clearingLabel
                    content.cacheCleared -> cacheClearedLabel
                    else -> clearCacheSubtitle
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
                headline = aboutLabel,
                supporting = aboutVersion,
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
            title = defaultCountryLabel,
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
    // stringResource is @Composable — resolve the three segment labels here,
    // then look them up from the plain (non-composable) displayLabel().
    val systemLabel = stringResource(Res.string.settings_theme_system)
    val lightLabel = stringResource(Res.string.settings_theme_light)
    val darkLabel = stringResource(Res.string.settings_theme_dark)
    fun ThemePreference.displayLabel(): String = when (this) {
        ThemePreference.SYSTEM -> systemLabel
        ThemePreference.LIGHT -> lightLabel
        ThemePreference.DARK -> darkLabel
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Euro.spacing.base, vertical = Euro.spacing.s),
    ) {
        Text(
            text = stringResource(Res.string.settings_theme_label),
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
    // English/Polski/Українська are endonyms and stay identical across every
    // locale (see strings.xml comment); only "System" is translated. Resolve
    // all four here — stringResource is @Composable — then look them up from
    // the plain (non-composable) label <-> code helpers.
    val systemLabel = stringResource(Res.string.settings_language_system)
    val englishLabel = stringResource(Res.string.settings_language_english)
    val polskiLabel = stringResource(Res.string.settings_language_polski)
    val ukrainskaLabel = stringResource(Res.string.settings_language_ukrainska)

    fun languageLabel(code: String): String = when (code) {
        "en" -> englishLabel
        "pl" -> polskiLabel
        "uk" -> ukrainskaLabel
        else -> systemLabel
    }

    /** Inverse of [languageLabel]; unknown labels fall back to `"system"`. */
    fun languageCodeFor(label: String): String = when (label) {
        englishLabel -> "en"
        polskiLabel -> "pl"
        ukrainskaLabel -> "uk"
        else -> "system"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Euro.spacing.base, vertical = Euro.spacing.s),
    ) {
        MetricDropdown(
            label = stringResource(Res.string.settings_language_label),
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

/** Flag + name + code line for the default-country row. */
private fun countryLabel(code: String): String {
    val country = EurostatCountries.byCode(code) ?: return code
    return "${country.flag} ${country.name} · ${country.code}"
}
