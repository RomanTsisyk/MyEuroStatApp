package eu.eurostat.feature.science.data

import eu.eurostat.core.jsonstat.JsonStatCell
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ScienceCellMapperTest {

    // ---------------------------------------------------------------------------
    // Helpers — minimal dimension maps that satisfy each mapper's filter predicate
    // ---------------------------------------------------------------------------

    private fun rdCell(geo: String, year: String, value: Double?) = JsonStatCell(
        dimensions = mapOf("geo" to geo, "time" to year, "unit" to "PC_GDP", "sectperf" to "TOTAL"),
        value = value,
    )

    private fun internetCell(geo: String, year: String, value: Double?) = JsonStatCell(
        dimensions = mapOf(
            "geo" to geo, "time" to year,
            "unit" to "PC_IND", "indic_is" to "I_IU3", "ind_type" to "IND_TOTAL",
        ),
        value = value,
    )

    private fun educCell(geo: String, year: String, value: Double?) = JsonStatCell(
        dimensions = mapOf(
            "geo" to geo, "time" to year,
            "unit" to "PC", "isced11" to "ED5-8", "sex" to "T", "age" to "Y25-64",
        ),
        value = value,
    )

    // ---------------------------------------------------------------------------
    // mapRdSpend
    // ---------------------------------------------------------------------------

    @Test
    fun mapRdSpend_extracts_country_year_and_value() {
        val cells = listOf(
            rdCell("PL", "2020", 1.21),
            rdCell("DE", "2021", 3.14),
        )
        val result = ScienceCellMapper.mapRdSpend(cells)
        assertEquals(2, result.size)
        assertEquals(1.21, result["PL" to 2020])
        assertEquals(3.14, result["DE" to 2021])
    }

    @Test
    fun mapRdSpend_preserves_null_values() {
        val cells = listOf(rdCell("PL", "2020", null))
        val result = ScienceCellMapper.mapRdSpend(cells)
        assertEquals(1, result.size)
        assertTrue(result.containsKey("PL" to 2020))
        assertNull(result["PL" to 2020])
    }

    @Test
    fun mapRdSpend_skips_cells_missing_geo() {
        val cell = JsonStatCell(
            dimensions = mapOf("time" to "2020", "unit" to "PC_GDP", "sectperf" to "TOTAL"),
            value = 2.5,
        )
        assertTrue(ScienceCellMapper.mapRdSpend(listOf(cell)).isEmpty())
    }

    @Test
    fun mapRdSpend_skips_cells_missing_time() {
        val cell = JsonStatCell(
            dimensions = mapOf("geo" to "PL", "unit" to "PC_GDP", "sectperf" to "TOTAL"),
            value = 2.5,
        )
        assertTrue(ScienceCellMapper.mapRdSpend(listOf(cell)).isEmpty())
    }

    @Test
    fun mapRdSpend_skips_cells_with_non_integer_time() {
        val cell = JsonStatCell(
            dimensions = mapOf("geo" to "PL", "time" to "Q2", "unit" to "PC_GDP", "sectperf" to "TOTAL"),
            value = 2.5,
        )
        assertTrue(ScienceCellMapper.mapRdSpend(listOf(cell)).isEmpty())
    }

    @Test
    fun mapRdSpend_empty_input_returns_empty_map() {
        assertTrue(ScienceCellMapper.mapRdSpend(emptyList()).isEmpty())
    }

    @Test
    fun mapRdSpend_filters_out_wrong_unit() {
        // unit=PC_IND should not pass the rd filter (which requires PC_GDP)
        val cell = JsonStatCell(
            dimensions = mapOf("geo" to "PL", "time" to "2020", "unit" to "PC_IND", "sectperf" to "TOTAL"),
            value = 2.5,
        )
        assertTrue(ScienceCellMapper.mapRdSpend(listOf(cell)).isEmpty())
    }

    // ---------------------------------------------------------------------------
    // mapInternetUsage
    // ---------------------------------------------------------------------------

    @Test
    fun mapInternetUsage_extracts_country_year_and_value() {
        val cells = listOf(
            internetCell("PL", "2020", 85.0),
            internetCell("FI", "2021", 92.3),
        )
        val result = ScienceCellMapper.mapInternetUsage(cells)
        assertEquals(2, result.size)
        assertEquals(85.0, result["PL" to 2020])
        assertEquals(92.3, result["FI" to 2021])
    }

    @Test
    fun mapInternetUsage_preserves_null_values() {
        val cells = listOf(internetCell("PL", "2020", null))
        val result = ScienceCellMapper.mapInternetUsage(cells)
        assertTrue(result.containsKey("PL" to 2020))
        assertNull(result["PL" to 2020])
    }

    @Test
    fun mapInternetUsage_empty_input_returns_empty_map() {
        assertTrue(ScienceCellMapper.mapInternetUsage(emptyList()).isEmpty())
    }

    @Test
    fun mapInternetUsage_filters_out_wrong_ind_type() {
        val cell = JsonStatCell(
            dimensions = mapOf(
                "geo" to "PL", "time" to "2020",
                "unit" to "PC_IND", "indic_is" to "I_IU3", "ind_type" to "HH_TOTAL",
            ),
            value = 85.0,
        )
        assertTrue(ScienceCellMapper.mapInternetUsage(listOf(cell)).isEmpty())
    }

    // ---------------------------------------------------------------------------
    // mapTertiaryEduc
    // ---------------------------------------------------------------------------

    @Test
    fun mapTertiaryEduc_extracts_country_year_and_value() {
        val cells = listOf(
            educCell("PL", "2020", 45.2),
            educCell("DE", "2021", 32.7),
        )
        val result = ScienceCellMapper.mapTertiaryEduc(cells)
        assertEquals(2, result.size)
        assertEquals(45.2, result["PL" to 2020])
        assertEquals(32.7, result["DE" to 2021])
    }

    @Test
    fun mapTertiaryEduc_preserves_null_values() {
        val cells = listOf(educCell("PL", "2020", null))
        val result = ScienceCellMapper.mapTertiaryEduc(cells)
        assertTrue(result.containsKey("PL" to 2020))
        assertNull(result["PL" to 2020])
    }

    @Test
    fun mapTertiaryEduc_empty_input_returns_empty_map() {
        assertTrue(ScienceCellMapper.mapTertiaryEduc(emptyList()).isEmpty())
    }

    @Test
    fun mapTertiaryEduc_filters_out_wrong_sex() {
        val cell = JsonStatCell(
            dimensions = mapOf(
                "geo" to "PL", "time" to "2020",
                "unit" to "PC", "isced11" to "ED5-8", "sex" to "M", "age" to "Y25-64",
            ),
            value = 45.2,
        )
        assertTrue(ScienceCellMapper.mapTertiaryEduc(listOf(cell)).isEmpty())
    }

    // ---------------------------------------------------------------------------
    // Simulated three-dataset merge (as ScienceApiServiceImpl does)
    // ---------------------------------------------------------------------------

    @Test
    fun three_maps_union_by_key_covers_all_rows() {
        // R&D only for PL-2020
        val rdMap = ScienceCellMapper.mapRdSpend(
            listOf(rdCell("PL", "2020", 1.21))
        )
        // Internet only for DE-2021
        val internetMap = ScienceCellMapper.mapInternetUsage(
            listOf(internetCell("DE", "2021", 85.0))
        )
        // Educ only for FR-2022
        val educMap = ScienceCellMapper.mapTertiaryEduc(
            listOf(educCell("FR", "2022", 47.3))
        )

        val allKeys = (rdMap.keys + internetMap.keys + educMap.keys).toSet()
        assertEquals(3, allKeys.size)

        val plKey = "PL" to 2020
        assertEquals(1.21, rdMap[plKey])
        assertNull(internetMap[plKey])
        assertNull(educMap[plKey])
    }

    @Test
    fun three_maps_all_present_for_same_key() {
        val geo = "FI"
        val year = "2019"
        val rdMap       = ScienceCellMapper.mapRdSpend(listOf(rdCell(geo, year, 2.8)))
        val internetMap = ScienceCellMapper.mapInternetUsage(listOf(internetCell(geo, year, 90.5)))
        val educMap     = ScienceCellMapper.mapTertiaryEduc(listOf(educCell(geo, year, 45.1)))

        val key = geo to year.toInt()
        assertEquals(2.8,  rdMap[key])
        assertEquals(90.5, internetMap[key])
        assertEquals(45.1, educMap[key])
    }

    @Test
    fun rd_only_map_yields_data_point_with_internet_and_educ_null() {
        val rdMap       = ScienceCellMapper.mapRdSpend(listOf(rdCell("PL", "2020", 1.0)))
        val internetMap = ScienceCellMapper.mapInternetUsage(emptyList())
        val educMap     = ScienceCellMapper.mapTertiaryEduc(emptyList())

        val key = "PL" to 2020
        assertEquals(1.0, rdMap[key])
        assertNull(internetMap[key])
        assertNull(educMap[key])
    }

    @Test
    fun mapRdSpend_handles_multiple_countries_and_years() {
        val countries = listOf("PL", "DE", "FR", "FI", "SE")
        val years = listOf("2018", "2019", "2020", "2021")
        val cells = countries.flatMap { c ->
            years.map { y -> rdCell(c, y, y.toDouble()) }
        }
        val result = ScienceCellMapper.mapRdSpend(cells)
        assertEquals(20, result.size)
        assertEquals(2020.0, result["PL" to 2020])
        assertEquals(2021.0, result["SE" to 2021])
    }
}
