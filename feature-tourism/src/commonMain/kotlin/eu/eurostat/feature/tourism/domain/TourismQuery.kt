package eu.eurostat.feature.tourism.domain

/**
 * Query for the tourism feature. The repository always fetches all three
 * residence categories together (DOM / FOR / TOTAL) so the stacked-bar UI can
 * render the full picture without re-issuing requests when the user toggles
 * which series is highlighted.
 */
data class TourismQuery(
    val countryCodes: List<String>,
    val yearRange: IntRange,
)
