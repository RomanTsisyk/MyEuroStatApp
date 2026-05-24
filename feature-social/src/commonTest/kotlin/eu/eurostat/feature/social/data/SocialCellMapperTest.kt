package eu.eurostat.feature.social.data

import eu.eurostat.core.jsonstat.JsonStatCell
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SocialCellMapperTest {

    // ---------------------------------------------------------------------------
    // mapToValueMap
    // ---------------------------------------------------------------------------

    @Test
    fun mapToValueMap_extracts_country_year_and_value() {
        val cells = listOf(
            JsonStatCell(mapOf("geo" to "PL", "time" to "2020"), value = 17.5),
            JsonStatCell(mapOf("geo" to "DE", "time" to "2021"), value = 14.2),
        )
        val result = SocialCellMapper.mapToValueMap(cells)
        assertEquals(2, result.size)
        assertEquals(17.5, result["PL" to 2020])
        assertEquals(14.2, result["DE" to 2021])
    }

    @Test
    fun mapToValueMap_preserves_null_values() {
        val cells = listOf(
            JsonStatCell(mapOf("geo" to "PL", "time" to "2020"), value = null),
        )
        val result = SocialCellMapper.mapToValueMap(cells)
        // The key should still be present with null value
        assertEquals(1, result.size)
        assertTrue(result.containsKey("PL" to 2020))
        assertNull(result["PL" to 2020])
    }

    @Test
    fun mapToValueMap_skips_cells_without_geo() {
        val cells = listOf(
            JsonStatCell(mapOf("time" to "2020"), value = 10.0),
        )
        val result = SocialCellMapper.mapToValueMap(cells)
        assertTrue(result.isEmpty())
    }

    @Test
    fun mapToValueMap_skips_cells_without_time() {
        val cells = listOf(
            JsonStatCell(mapOf("geo" to "PL"), value = 10.0),
        )
        val result = SocialCellMapper.mapToValueMap(cells)
        assertTrue(result.isEmpty())
    }

    @Test
    fun mapToValueMap_skips_cells_with_non_integer_time() {
        val cells = listOf(
            JsonStatCell(mapOf("geo" to "PL", "time" to "Q1"), value = 10.0),
        )
        val result = SocialCellMapper.mapToValueMap(cells)
        assertTrue(result.isEmpty())
    }

    @Test
    fun mapToValueMap_empty_input_returns_empty_map() {
        val result = SocialCellMapper.mapToValueMap(emptyList())
        assertTrue(result.isEmpty())
    }

    @Test
    fun mapToValueMap_handles_multiple_countries_and_years() {
        val countries = listOf("PL", "DE", "FR", "ES")
        val years = listOf("2019", "2020", "2021")
        val cells = countries.flatMap { c ->
            years.map { y ->
                JsonStatCell(mapOf("geo" to c, "time" to y), value = c.hashCode().toDouble() + y.toInt())
            }
        }
        val result = SocialCellMapper.mapToValueMap(cells)
        assertEquals(12, result.size)
        assertEquals("PL".hashCode().toDouble() + 2020, result["PL" to 2020])
    }

    // ---------------------------------------------------------------------------
    // Simulated merging (as the service would do) — validate map composition
    // ---------------------------------------------------------------------------

    @Test
    fun three_independent_maps_union_by_key_covers_all_rows() {
        // Poverty-only row for PL-2020
        val povertyMap = SocialCellMapper.mapToValueMap(
            listOf(JsonStatCell(mapOf("geo" to "PL", "time" to "2020"), value = 17.5))
        )
        // At-risk-only row for DE-2021
        val atRiskMap = SocialCellMapper.mapToValueMap(
            listOf(JsonStatCell(mapOf("geo" to "DE", "time" to "2021"), value = 21.0))
        )
        // Health-only row for FR-2022
        val healthMap = SocialCellMapper.mapToValueMap(
            listOf(JsonStatCell(mapOf("geo" to "FR", "time" to "2022"), value = 65.3))
        )

        val allKeys = (povertyMap.keys + atRiskMap.keys + healthMap.keys).toSet()
        assertEquals(3, allKeys.size)

        val plKey = "PL" to 2020
        assertEquals(17.5, povertyMap[plKey])
        assertNull(atRiskMap[plKey])
        assertNull(healthMap[plKey])

        val deKey = "DE" to 2021
        assertNull(povertyMap[deKey])
        assertEquals(21.0, atRiskMap[deKey])
        assertNull(healthMap[deKey])
    }

    @Test
    fun three_maps_all_present_for_same_key() {
        val cell = listOf(JsonStatCell(mapOf("geo" to "PL", "time" to "2020"), value = 10.0))
        val povertyMap = SocialCellMapper.mapToValueMap(cell)
        val atRiskMap  = SocialCellMapper.mapToValueMap(listOf(
            JsonStatCell(mapOf("geo" to "PL", "time" to "2020"), value = 20.0)
        ))
        val healthMap  = SocialCellMapper.mapToValueMap(listOf(
            JsonStatCell(mapOf("geo" to "PL", "time" to "2020"), value = 30.0)
        ))

        val key = "PL" to 2020
        assertEquals(10.0, povertyMap[key])
        assertEquals(20.0, atRiskMap[key])
        assertEquals(30.0, healthMap[key])
    }

    // ---------------------------------------------------------------------------
    // mapHealthToValueMap — deterministic filter for hlth_silc_01
    // ---------------------------------------------------------------------------

    private fun healthCell(
        geo: String,
        time: String,
        levels: String = "VGOOD",
        sex: String = "T",
        age: String = "Y_GE16",
        wstatus: String = "POP",
        value: Double? = 19.7,
    ) = JsonStatCell(
        mapOf("geo" to geo, "time" to time, "levels" to levels, "sex" to sex, "age" to age, "wstatus" to wstatus),
        value = value,
    )

    @Test
    fun mapHealthToValueMap_accepts_target_slice() {
        val cells = listOf(healthCell("DE", "2020"))
        val result = SocialCellMapper.mapHealthToValueMap(cells)
        assertEquals(1, result.size)
        assertEquals(19.7, result["DE" to 2020])
    }

    @Test
    fun mapHealthToValueMap_skips_non_VGOOD_levels() {
        val cells = listOf(healthCell("DE", "2020", levels = "GOOD"))
        assertTrue(SocialCellMapper.mapHealthToValueMap(cells).isEmpty())
    }

    @Test
    fun mapHealthToValueMap_skips_non_total_sex() {
        val cells = listOf(healthCell("DE", "2020", sex = "M"))
        assertTrue(SocialCellMapper.mapHealthToValueMap(cells).isEmpty())
    }

    @Test
    fun mapHealthToValueMap_skips_non_YGE16_age() {
        val cells = listOf(healthCell("DE", "2020", age = "Y16-24"))
        assertTrue(SocialCellMapper.mapHealthToValueMap(cells).isEmpty())
    }

    @Test
    fun mapHealthToValueMap_skips_non_POP_wstatus() {
        val cells = listOf(healthCell("DE", "2020", wstatus = "EMP"))
        assertTrue(SocialCellMapper.mapHealthToValueMap(cells).isEmpty())
    }

    @Test
    fun mapHealthToValueMap_is_deterministic_with_multiple_dimension_values() {
        val cells = listOf(
            healthCell("DE", "2020", levels = "GOOD",  sex = "T",   age = "Y_GE16", wstatus = "POP", value = 99.0),
            healthCell("DE", "2020", levels = "VGOOD", sex = "M",   age = "Y_GE16", wstatus = "POP", value = 88.0),
            healthCell("DE", "2020", levels = "VGOOD", sex = "T",   age = "Y16-24", wstatus = "POP", value = 77.0),
            healthCell("DE", "2020", levels = "VGOOD", sex = "T",   age = "Y_GE16", wstatus = "EMP", value = 66.0),
            healthCell("DE", "2020", levels = "VGOOD", sex = "T",   age = "Y_GE16", wstatus = "POP", value = 19.7),
        )
        val result = SocialCellMapper.mapHealthToValueMap(cells)
        assertEquals(1, result.size)
        assertEquals(19.7, result["DE" to 2020])
    }
}
