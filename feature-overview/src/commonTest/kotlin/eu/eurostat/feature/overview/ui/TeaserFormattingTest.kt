package eu.eurostat.feature.overview.ui

import eu.eurostat.core.navigation.ChildConfig
import myeurostatapp.feature_overview.generated.resources.Res
import myeurostatapp.feature_overview.generated.resources.overview_module_title_economy
import myeurostatapp.feature_overview.generated.resources.overview_module_unit_economy
import kotlin.test.Test
import kotlin.test.assertEquals

/** Pure formatting of raw teaser values; explicit locale tags keep the results deterministic. */
class TeaserFormattingTest {

    @Test
    fun compact_uses_kmb_suffixes_and_locale_decimal_separator() {
        assertEquals("83.5M", formatTeaserValue(83_500_000.0, TeaserFormat.Compact, "en"))
        assertEquals("83,5M", formatTeaserValue(83_500_000.0, TeaserFormat.Compact, "de"))
        assertEquals("850K", formatTeaserValue(850_000.0, TeaserFormat.Compact, "en"))
    }

    @Test
    fun millions_of_euro_are_shown_as_whole_billions() {
        assertEquals("4,387", formatTeaserValue(4_387_400.0, TeaserFormat.BillionsFromMillions, "en"))
        // Rounds to the nearest billion, not truncating.
        assertEquals("4,388", formatTeaserValue(4_387_600.0, TeaserFormat.BillionsFromMillions, "en"))
        assertEquals("840", formatTeaserValue(840_100.0, TeaserFormat.BillionsFromMillions, "en"))
    }

    @Test
    fun grouped_rounds_to_a_whole_number() {
        assertEquals("650", formatTeaserValue(650.4, TeaserFormat.Grouped, "en"))
        assertEquals("651", formatTeaserValue(650.6, TeaserFormat.Grouped, "en"))
    }

    @Test
    fun percent_has_one_decimal_and_locale_separator() {
        assertEquals("15.5%", formatTeaserValue(15.5, TeaserFormat.Percent, "en"))
        assertEquals("15,5%", formatTeaserValue(15.5, TeaserFormat.Percent, "pl"))
        assertEquals("3.1%", formatTeaserValue(3.1, TeaserFormat.Percent, "en"))
    }

    @Test
    fun display_value_falls_back_to_dash_without_a_value() {
        val teaser = ModuleTeaser(
            destination = ChildConfig.Economy,
            accentKey = "economy",
            titleRes = Res.string.overview_module_title_economy,
            emoji = "x",
            value = null,
            format = TeaserFormat.BillionsFromMillions,
            unitRes = Res.string.overview_module_unit_economy,
            year = null,
            status = TeaserStatus.Loading,
        )
        assertEquals("—", teaser.displayValue("en"))
        assertEquals("4,387", teaser.copy(value = 4_387_400.0).displayValue("en"))
    }
}
