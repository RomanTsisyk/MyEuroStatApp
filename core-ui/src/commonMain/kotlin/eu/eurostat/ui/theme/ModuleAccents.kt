package eu.eurostat.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Per-module accent colors. Each feature module gets one of eight desaturated
 * hues so that lists / chips / kpis can be distinguished without using the
 * loud EU flag blue.
 *
 * The hues match the hi-fi spec; light and dark themes each have their own
 * variant (selected by [EurostatTheme] via [lightModuleAccents] /
 * [darkModuleAccents]).
 *
 * Use [forModule] for a tolerant lookup — it accepts both the canonical key
 * (e.g. "people") and the user-facing screen title (e.g. "Population"), and
 * is case-insensitive.
 */
data class ModuleAccents(
    val economy: Color,
    val people: Color,
    val climate: Color,
    val trade: Color,
    val transport: Color,
    val tourism: Color,
    val social: Color,
    val science: Color,
) {
    /**
     * Resolve a module accent by name. Maps both internal keys and screen
     * titles to the right accent. Falls back to [economy] when [name] is
     * unrecognised.
     */
    fun forModule(name: String): Color = when (name.trim().lowercase()) {
        "economy" -> economy
        "people", "population" -> people
        "climate", "environment" -> climate
        "trade" -> trade
        "transport" -> transport
        "tourism" -> tourism
        "social" -> social
        "science" -> science
        else -> economy
    }
}

/** Light-theme module accents (desaturated, used on warm paper). */
fun lightModuleAccents(): ModuleAccents = ModuleAccents(
    economy = Color(0xFF2F4969),
    people = Color(0xFF5E6B58),
    climate = Color(0xFF6B8A76),
    trade = Color(0xFF7A5C46),
    transport = Color(0xFF4A4A55),
    tourism = Color(0xFFB06A3A),
    social = Color(0xFF7D5E76),
    science = Color(0xFF4A6B7A),
)

/** Dark-theme module accents (lifted for contrast against near-black). */
fun darkModuleAccents(): ModuleAccents = ModuleAccents(
    economy = Color(0xFF7DA0C9),
    people = Color(0xFF9CB394),
    climate = Color(0xFF9FC3AC),
    trade = Color(0xFFC2967A),
    transport = Color(0xFFA8A8B8),
    tourism = Color(0xFFE69664),
    social = Color(0xFFC599BC),
    science = Color(0xFF8FB4C7),
)
