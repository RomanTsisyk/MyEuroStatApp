package eu.eurostat.feature.compare.ui

import eu.eurostat.feature.compare.domain.CompareIndicator

/** User actions on the Compare screen. */
sealed interface CompareIntent {

    /** Switches the compared indicator; triggers a re-fetch from the new module's repository. */
    data class SelectIndicator(val indicator: CompareIndicator) : CompareIntent

    /**
     * Replaces the set of compared countries (in the given order); triggers a
     * re-fetch. The list is de-duplicated and clamped to the supported range by
     * the component.
     */
    data class SelectCountries(val codes: List<String>) : CompareIntent

    /**
     * Toggles the chart between absolute values and the "Indexed 100" rebasing.
     * A pure view transform — never triggers a re-fetch.
     */
    data object ToggleNormalization : CompareIntent

    /** Re-observes the current selection (re-checks cache freshness / re-fetches). */
    data object Refresh : CompareIntent
}
