package eu.eurostat.core.charts.model

import androidx.compose.ui.graphics.Color

/**
 * Ordered categorical palette for multi-series charts (one color per series).
 *
 * Eight editorial-muted, mid-value hues chosen to stay legible on both the
 * light warm-paper background (`#F4F1EA`) and the dark near-black background
 * (`#0E0D0C`) of the design system. The temperature extends the hand-picked
 * series colors originally used on the Economy screen (sienna `#7A5C46`,
 * olive `#5E6B58`, muted sand `#A39A8D`). Adjacent entries are kept hue- and
 * value-distinct so small selections (2–4 series) read clearly at a glance;
 * the lowest-contrast greige is deliberately last.
 *
 * Assign colors by the series' stable index in the rendered list via
 * [colorAt] — never by country code — so any set of picked series gets
 * distinct colors without hardcoded mappings.
 */
object SeriesPalette {

    /** Palette entries in assignment order. */
    val colors: List<Color> = listOf(
        Color(0xFF5B7A99), // slate blue — kin of the app accent, lifted to mid-value
        Color(0xFF7A5C46), // sienna
        Color(0xFF5E6B58), // olive
        Color(0xFFB4694A), // terracotta
        Color(0xFF7E6478), // dusty plum
        Color(0xFF4F7B76), // muted teal
        Color(0xFFA8894E), // ochre
        Color(0xFFA39A8D), // muted sand — lowest contrast, deliberately last
    )

    /**
     * Returns the palette color for the series at [index], cycling when the
     * index reaches the palette size. Negative indices are safe too and cycle
     * backwards from the end (Euclidean modulo), so any [Int] maps to a color.
     */
    fun colorAt(index: Int): Color = colors[index.mod(colors.size)]
}
