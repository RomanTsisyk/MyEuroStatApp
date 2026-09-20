package eu.eurostat.ui.country

import eu.eurostat.core.common.EurostatCountries
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Locks down [countryNameRes], the code -> string-resource mapping behind
 * [countryDisplayName]. The composable itself needs a Compose host, but the
 * mapping is a plain function, so coverage gaps are caught here on the JVM.
 */
class CountryNameResTest {

    @Test
    fun every_catalogue_code_has_a_name_resource() {
        EurostatCountries.ALL.forEach { country ->
            assertNotNull(
                countryNameRes(country.code),
                "No country_* string mapped for Eurostat code '${country.code}'",
            )
        }
    }

    @Test
    fun resource_key_follows_country_lowercase_code_convention() {
        EurostatCountries.ALL.forEach { country ->
            val res = assertNotNull(countryNameRes(country.code))
            assertEquals("country_${country.code.lowercase()}", res.key, "code '${country.code}'")
        }
    }

    @Test
    fun each_code_maps_to_its_own_resource() {
        val keys = EurostatCountries.ALL.mapNotNull { countryNameRes(it.code)?.key }
        assertEquals(EurostatCountries.ALL.size, keys.toSet().size)
        assertTrue(keys.isNotEmpty())
    }

    @Test
    fun unknown_code_has_no_mapping() {
        assertNull(countryNameRes("XX"))
        assertNull(countryNameRes(""))
        assertNull(countryNameRes("GR")) // Eurostat uses "EL"
    }

    @Test
    fun lookup_is_case_sensitive_like_byCode() {
        assertNull(countryNameRes("de"))
        assertNull(EurostatCountries.byCode("de"))
    }
}
