package eu.eurostat.core.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Integrity tests for the static [EurostatCountries] catalogue and [EurostatCountry].
 */
class EurostatCountriesTest {

    @Test
    fun catalogue_has_expected_size() {
        // 2 aggregates (EU27_2020, EA20) + 27 EU members + 5 EFTA/other = 34.
        assertEquals(34, EurostatCountries.ALL.size)
    }

    @Test
    fun all_codes_are_unique() {
        val codes = EurostatCountries.ALL.map { it.code }
        assertEquals(codes.size, codes.toSet().size, "Duplicate country code(s): $codes")
    }

    @Test
    fun every_entry_has_non_blank_code_and_name() {
        EurostatCountries.ALL.forEach {
            assertTrue(it.code.isNotBlank(), "Blank code for $it")
            assertTrue(it.name.isNotBlank(), "Blank name for $it")
        }
    }

    @Test
    fun byCode_resolves_known_countries() {
        assertEquals("Germany", EurostatCountries.byCode("DE")?.name)
        assertEquals("Greece", EurostatCountries.byCode("EL")?.name)
        assertEquals("United Kingdom", EurostatCountries.byCode("UK")?.name)
        assertEquals("EU (27)", EurostatCountries.byCode("EU27_2020")?.name)
    }

    @Test
    fun byCode_is_case_sensitive_and_returns_null_for_unknown() {
        assertNull(EurostatCountries.byCode("XX"))
        assertNull(EurostatCountries.byCode("de")) // lowercase must not match "DE"
    }

    @Test
    fun catalogue_contains_aggregates_and_sample_members() {
        val codes = EurostatCountries.ALL.map { it.code }.toSet()
        listOf("EU27_2020", "EA20", "DE", "FR", "IT", "ES", "PL", "SE", "NO", "CH").forEach {
            assertTrue(it in codes, "Expected $it in the catalogue")
        }
    }

    @Test
    fun flag_defaults_to_flagFor_of_the_code() {
        // The data-class default computes the flag from the code via flagFor().
        val germany = assertNotNull(EurostatCountries.byCode("DE"))
        assertEquals(flagFor("DE"), germany.flag)
        assertTrue(germany.flag.isNotBlank())
    }
}
