package eu.eurostat.feature.settings.ui

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Locks the pure text-building half of the Settings default-country row. The
 * localized name is resolved by the @Composable `countryDisplayName` in
 * `SettingsContent` and passed in, so [countryLabel] must render exactly the
 * name it is given (never an English catalogue name of its own).
 */
class SettingsCountryLabelTest {

    @Test
    fun countryLabel_uses_the_polish_name_it_is_given() {
        assertEquals("Niemcy · DE", countryLabel("DE", "Niemcy"))
    }

    @Test
    fun countryLabel_uses_the_ukrainian_name_it_is_given() {
        assertEquals("Греція · EL", countryLabel("EL", "Греція"))
    }

    @Test
    fun countryLabel_renders_english_only_when_english_is_passed() {
        assertEquals("Germany · DE", countryLabel("DE", "Germany"))
    }

    @Test
    fun countryLabel_falls_back_to_bare_code_when_name_equals_code() {
        assertEquals("XX", countryLabel("XX", "XX"))
    }

    @Test
    fun countryLabel_falls_back_to_bare_code_when_name_is_blank() {
        assertEquals("XX", countryLabel("XX", ""))
        assertEquals("XX", countryLabel("XX", "   "))
    }
}
