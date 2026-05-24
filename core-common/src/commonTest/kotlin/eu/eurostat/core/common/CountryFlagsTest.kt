package eu.eurostat.core.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CountryFlagsTest {

    @Test
    fun flagFor_DE_returns_german_flag() {
        val flag = flagFor("DE")
        // Flag emoji are supplementary-plane characters encoded as surrogate pairs.
        // Minimum length for a 2-regional-indicator sequence: 2 surrogate pairs = 4 chars.
        assertEquals(4, flag.length, "Flag for DE should be 4 chars (2 surrogate pairs)")
        // High surrogate for region flag: always 0xD83C
        assertEquals(0xD83C.toChar(), flag[0], "First char should be high surrogate")
    }

    @Test
    fun flagFor_PL_returns_polish_flag() {
        val flag = flagFor("PL")
        assertEquals(4, flag.length)
    }

    @Test
    fun flagFor_EL_maps_to_greek_flag() {
        // Eurostat uses "EL" for Greece; flagFor must remap to "GR"
        val elFlag = flagFor("EL")
        val grFlag = flagFor("GR")
        assertEquals(elFlag, grFlag, "EL and GR should produce the same Greek flag emoji")
    }

    @Test
    fun flagFor_UK_maps_to_GB_flag() {
        // Eurostat uses "UK" for United Kingdom; flagFor must remap to "GB"
        val ukFlag = flagFor("UK")
        val gbFlag = flagFor("GB")
        assertEquals(ukFlag, gbFlag, "UK and GB should produce the same Union Jack emoji")
    }

    @Test
    fun flagFor_EU27_2020_returns_eu_flag() {
        val flag = flagFor("EU27_2020")
        assertEquals(4, flag.length, "EU flag should be 4 chars (2 surrogate pairs)")
    }

    @Test
    fun flagFor_EU_returns_eu_flag() {
        val flag = flagFor("EU")
        assertEquals(flagFor("EU27_2020"), flag, "EU and EU27_2020 should produce the same flag")
    }

    @Test
    fun flagFor_EA20_returns_EA_text() {
        val flag = flagFor("EA20")
        assertEquals("EA", flag, "Euro area has no emoji; should return 'EA' text")
    }

    @Test
    fun flagFor_EA_returns_EA_text() {
        val flag = flagFor("EA")
        assertEquals("EA", flag)
    }

    @Test
    fun flagFor_eu27_2020_lowercase_still_works() {
        val flag = flagFor("eu27_2020")
        assertEquals(flagFor("EU27_2020"), flag, "Case-insensitive mapping should produce same flag")
    }

    @Test
    fun flagFor_empty_string_returns_empty() {
        val flag = flagFor("")
        assertEquals("", flag)
    }

    @Test
    fun flagFor_single_character_returns_input() {
        val flag = flagFor("X")
        assertEquals("X", flag)
    }

    @Test
    fun flagFor_three_character_code_returns_input() {
        val flag = flagFor("USA")
        assertEquals("USA", flag, "Three-letter codes should not be treated as ISO codes")
    }

    @Test
    fun flagFor_numeric_code_returns_input() {
        val flag = flagFor("12")
        assertEquals("12", flag, "Numeric codes should not produce flag emoji")
    }

    @Test
    fun flagFor_DE_does_not_equal_raw_text() {
        val flag = flagFor("DE")
        assertTrue(flag != "DE", "Flag for DE should be an emoji, not the raw text 'DE'")
    }

    @Test
    fun isoToFlag_DE_produces_emoji() {
        val flag = isoToFlag("DE")
        assertEquals(4, flag.length, "Regional indicator sequence should be 4 chars")
    }

    @Test
    fun isoToFlag_empty_returns_empty() {
        assertEquals("", isoToFlag(""))
    }

    @Test
    fun isoToFlag_single_char_returns_same() {
        assertEquals("X", isoToFlag("X"))
    }

    @Test
    fun isoToFlag_three_chars_returns_same() {
        assertEquals("ABC", isoToFlag("ABC"))
    }

    @Test
    fun isoToFlag_lowercase_produces_correct_emoji() {
        assertEquals(isoToFlag("DE"), isoToFlag("de"), "Uppercase and lowercase should produce same emoji")
    }

    @Test
    fun isoToFlag_non_alpha_returns_same() {
        assertEquals("12", isoToFlag("12"))
    }

    @Test
    fun isoToFlag_mixed_case_produces_same_as_uppercase() {
        assertEquals(isoToFlag("PL"), isoToFlag("pl"))
        assertEquals(isoToFlag("Pl"), isoToFlag("pL"))
    }

    @Test
    fun flagFor_TR_returns_turkish_flag() {
        val flag = flagFor("TR")
        assertEquals(4, flag.length)
    }

    @Test
    fun flagFor_known_countries_produce_distinct_flags() {
        val flags = listOf("DE", "FR", "PL", "IT", "ES", "NL", "BE", "AT")
            .map { flagFor(it) }
        // All should be 4 chars (2 surrogate pairs)
        flags.forEach { assertEquals(4, it.length, "Flag should be 4 chars") }
        // All should be distinct strings
        assertEquals(flags.toSet().size, flags.size, "Every country should have a distinct flag")
    }

    @Test
    fun flagFor_EL_and_GR_produce_matching_flags() {
        assertEquals(flagFor("EL"), flagFor("GR"))
    }

    @Test
    fun flagFor_UK_and_GB_produce_matching_flags() {
        assertEquals(flagFor("UK"), flagFor("GB"))
    }

    @Test
    fun flagFor_EL_and_GR_are_identical_to_isoToFlag_GR() {
        assertEquals(flagFor("EL"), isoToFlag("GR"))
    }

    @Test
    fun flagFor_UK_and_GB_are_identical_to_isoToFlag_GB() {
        assertEquals(flagFor("UK"), isoToFlag("GB"))
    }
}
