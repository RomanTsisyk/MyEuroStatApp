package eu.eurostat.feature.population.data

import eu.eurostat.core.jsonstat.JsonStatCell
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [PopulationCellMapper].
 *
 * Dimension values use CODES (keys from JSON-stat category.index), not labels,
 * because JsonStatParser.buildLabelLookups resolves via category.index keys.
 * So dimensions["geo"] = "PL", dimensions["sex"] = "T", etc.
 */
class PopulationCellMapperTest {

    // ------------------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------------------

    private fun cell(geo: String, time: String, sex: String, age: String = "TOTAL", value: Double?) =
        JsonStatCell(
            dimensions = mapOf("geo" to geo, "time" to time, "sex" to sex, "age" to age),
            value = value,
        )

    // ------------------------------------------------------------------------------------
    // 1. Happy path: 2 countries × 2 years
    // ------------------------------------------------------------------------------------

    @Test
    fun map_happyPath_twoCountriesTwoYears_producesCorrectTimeSeries() {
        val cells = listOf(
            cell("PL", "2020", "T", value = 38_000_000.0),
            cell("PL", "2020", "M", value = 18_000_000.0),
            cell("PL", "2020", "F", value = 20_000_000.0),
            cell("PL", "2021", "T", value = 37_900_000.0),
            cell("PL", "2021", "M", value = 17_900_000.0),
            cell("PL", "2021", "F", value = 20_000_000.0),
            cell("DE", "2020", "T", value = 83_000_000.0),
            cell("DE", "2020", "M", value = 41_000_000.0),
            cell("DE", "2020", "F", value = 42_000_000.0),
            cell("DE", "2021", "T", value = 83_200_000.0),
            cell("DE", "2021", "M", value = 41_100_000.0),
            cell("DE", "2021", "F", value = 42_100_000.0),
        )

        val result = PopulationCellMapper.map(cells)

        assertEquals(2, result.size, "Should produce 2 TimeSeries (one per country)")
        val pl = result.first { it.countryCode == "PL" }
        val de = result.first { it.countryCode == "DE" }
        assertEquals(2, pl.points.size)
        assertEquals(2, de.points.size)
    }

    // ------------------------------------------------------------------------------------
    // 2. Correct field values for a single data point
    // ------------------------------------------------------------------------------------

    @Test
    fun map_singlePoint_populatesAllSexFields() {
        val cells = listOf(
            cell("PL", "2020", "T", value = 38_000_000.0),
            cell("PL", "2020", "M", value = 18_000_000.0),
            cell("PL", "2020", "F", value = 20_000_000.0),
        )

        val result = PopulationCellMapper.map(cells)

        assertEquals(1, result.size)
        val point = result[0].points[0]
        assertEquals("PL", point.countryCode)
        assertEquals(2020, point.year)
        assertEquals(38_000_000L, point.totalPopulation)
        assertEquals(18_000_000L, point.malePopulation)
        assertEquals(20_000_000L, point.femalePopulation)
    }

    // ------------------------------------------------------------------------------------
    // 3. Missing sex=M → malePopulation null, others still populated
    // ------------------------------------------------------------------------------------

    @Test
    fun map_missingMaleCell_malePopulationIsNull() {
        val cells = listOf(
            cell("PL", "2020", "T", value = 38_000_000.0),
            // sex=M missing intentionally
            cell("PL", "2020", "F", value = 20_000_000.0),
        )

        val result = PopulationCellMapper.map(cells)

        assertEquals(1, result.size)
        val point = result[0].points[0]
        assertEquals(38_000_000L, point.totalPopulation)
        assertNull(point.malePopulation, "Male population should be null when sex=M cell is absent")
        assertEquals(20_000_000L, point.femalePopulation)
    }

    // ------------------------------------------------------------------------------------
    // 4. Missing sex=F → femalePopulation null
    // ------------------------------------------------------------------------------------

    @Test
    fun map_missingFemaleCell_femalePopulationIsNull() {
        val cells = listOf(
            cell("PL", "2020", "T", value = 38_000_000.0),
            cell("PL", "2020", "M", value = 18_000_000.0),
            // sex=F missing intentionally
        )

        val result = PopulationCellMapper.map(cells)

        val point = result[0].points[0]
        assertNull(point.femalePopulation, "Female population should be null when sex=F cell is absent")
        assertNotNull(point.malePopulation)
    }

    // ------------------------------------------------------------------------------------
    // 5. Null value in cell → field stored as null (not zero, not skipped for M/F)
    // ------------------------------------------------------------------------------------

    @Test
    fun map_nullValueInSexMCell_malePopulationIsNull() {
        val cells = listOf(
            cell("PL", "2020", "T", value = 38_000_000.0),
            cell("PL", "2020", "M", value = null), // explicit null observation
            cell("PL", "2020", "F", value = 20_000_000.0),
        )

        val result = PopulationCellMapper.map(cells)

        val point = result[0].points[0]
        assertNull(point.malePopulation, "Null value cell should produce null malePopulation, not zero")
        assertNotNull(point.femalePopulation)
    }

    // ------------------------------------------------------------------------------------
    // 6. Row without sex=T is SKIPPED (no total → can't produce a data point)
    // ------------------------------------------------------------------------------------

    @Test
    fun map_missingTotal_rowIsSkipped() {
        val cells = listOf(
            // sex=T missing — mapper skips rows without total
            cell("PL", "2020", "M", value = 18_000_000.0),
            cell("PL", "2020", "F", value = 20_000_000.0),
        )

        val result = PopulationCellMapper.map(cells)

        assertTrue(result.isEmpty(), "Rows without sex=T total should be skipped entirely")
    }

    // ------------------------------------------------------------------------------------
    // 7. Empty input → empty output
    // ------------------------------------------------------------------------------------

    @Test
    fun map_emptyInput_returnsEmpty() {
        val result = PopulationCellMapper.map(emptyList())
        assertTrue(result.isEmpty())
    }

    // ------------------------------------------------------------------------------------
    // 8. Points are sorted by year within a TimeSeries
    // ------------------------------------------------------------------------------------

    @Test
    fun map_pointsSortedAscendingByYear() {
        val cells = listOf(
            cell("PL", "2022", "T", value = 37_700_000.0),
            cell("PL", "2022", "M", value = 18_000_000.0),
            cell("PL", "2022", "F", value = 19_700_000.0),
            cell("PL", "2020", "T", value = 38_000_000.0),
            cell("PL", "2020", "M", value = 18_500_000.0),
            cell("PL", "2020", "F", value = 19_500_000.0),
            cell("PL", "2021", "T", value = 37_900_000.0),
            cell("PL", "2021", "M", value = 18_200_000.0),
            cell("PL", "2021", "F", value = 19_700_000.0),
        )

        val result = PopulationCellMapper.map(cells)
        val years = result[0].points.map { it.year }

        assertEquals(listOf(2020, 2021, 2022), years, "Points should be sorted ascending by year")
    }

    // ------------------------------------------------------------------------------------
    // 9. TimeSeries sorted by countryCode
    // ------------------------------------------------------------------------------------

    @Test
    fun map_timeSeriesSortedByCountryCode() {
        val cells = listOf(
            cell("PL", "2020", "T", value = 38_000_000.0),
            cell("PL", "2020", "M", value = 18_000_000.0),
            cell("PL", "2020", "F", value = 20_000_000.0),
            cell("DE", "2020", "T", value = 83_000_000.0),
            cell("DE", "2020", "M", value = 41_000_000.0),
            cell("DE", "2020", "F", value = 42_000_000.0),
            cell("FR", "2020", "T", value = 67_000_000.0),
            cell("FR", "2020", "M", value = 32_000_000.0),
            cell("FR", "2020", "F", value = 35_000_000.0),
        )

        val result = PopulationCellMapper.map(cells)
        val codes = result.map { it.countryCode }

        assertEquals(listOf("DE", "FR", "PL"), codes, "TimeSeries should be sorted alphabetically by countryCode")
    }

    // ------------------------------------------------------------------------------------
    // 10. Cells with invalid time strings are skipped
    // ------------------------------------------------------------------------------------

    @Test
    fun map_invalidTimeString_cellSkipped() {
        val cells = listOf(
            cell("PL", "not-a-year", "T", value = 38_000_000.0),
            cell("PL", "not-a-year", "M", value = 18_000_000.0),
            cell("PL", "not-a-year", "F", value = 20_000_000.0),
            // Valid cells
            cell("PL", "2020", "T", value = 38_000_000.0),
            cell("PL", "2020", "M", value = 18_000_000.0),
            cell("PL", "2020", "F", value = 20_000_000.0),
        )

        val result = PopulationCellMapper.map(cells)

        // Only the 2020 row should survive
        assertEquals(1, result.size)
        assertEquals(1, result[0].points.size)
        assertEquals(2020, result[0].points[0].year)
    }

    // ====================================================================================
    // mapToSnapshots tests — regression prevention for the pyramid "always loading" bug
    // ====================================================================================

    /**
     * Builds a full set of cells for one (country, year) with all 18 5-year cohort codes
     * plus sex=T TOTAL row. Mirrors the shape returned by demo_pjangroup.
     */
    private fun fullCohortCells(
        geo: String,
        time: String,
        cohortPopulations: List<Triple<String, Long, Long>>, // (ageCode, male, female)
        totalMale: Long,
        totalFemale: Long,
    ): List<JsonStatCell> {
        val cells = mutableListOf<JsonStatCell>()
        // TOTAL rows for sex=T/M/F at age=TOTAL
        val total = totalMale + totalFemale
        cells += cell(geo, time, "T", "TOTAL", total.toDouble())
        cells += cell(geo, time, "M", "TOTAL", totalMale.toDouble())
        cells += cell(geo, time, "F", "TOTAL", totalFemale.toDouble())
        // Cohort rows for sex=M and sex=F
        for ((ageCode, male, female) in cohortPopulations) {
            cells += cell(geo, time, "M", ageCode, male.toDouble())
            cells += cell(geo, time, "F", ageCode, female.toDouble())
        }
        return cells
    }

    /** Generates a plausible set of 18 cohort entries with synthetic population counts. */
    private fun syntheticCohorts(): List<Triple<String, Long, Long>> {
        val codes = listOf(
            "Y_LT5", "Y5-9", "Y10-14", "Y15-19", "Y20-24",
            "Y25-29", "Y30-34", "Y35-39", "Y40-44", "Y45-49",
            "Y50-54", "Y55-59", "Y60-64", "Y65-69", "Y70-74",
            "Y75-79", "Y80-84", "Y_GE85",
        )
        return codes.mapIndexed { i, code ->
            Triple(code, (500_000L + i * 10_000L), (490_000L + i * 10_000L))
        }
    }

    // ------------------------------------------------------------------------------------
    // 11. mapToSnapshots: non-empty result from realistic 18-cohort input
    //     This is the primary regression test for the pyramid "always loading" bug.
    // ------------------------------------------------------------------------------------

    @Test
    fun mapToSnapshots_18Cohorts_producesNonEmptySnapshotMap() {
        val cohorts = syntheticCohorts()
        val cells = fullCohortCells("DE", "2023", cohorts, 41_000_000L, 42_000_000L)

        val result = PopulationCellMapper.mapToSnapshots(cells)

        assertFalse(result.isEmpty(), "mapToSnapshots must produce a non-empty map for 18-cohort input")
        val key = "DE" to 2023
        val snapshot = result[key]
        assertNotNull(snapshot, "Snapshot must exist for key (DE, 2023)")
        assertEquals(18, snapshot.cohorts.size, "Snapshot must have all 18 cohort entries")
    }

    // ------------------------------------------------------------------------------------
    // 12. mapToSnapshots: snapshot key shape — must be Pair<String, Int> matching buildContent
    // ------------------------------------------------------------------------------------

    @Test
    fun mapToSnapshots_keyShape_isPairStringInt() {
        val cohorts = syntheticCohorts()
        val cells = fullCohortCells("FR", "2022", cohorts, 32_000_000L, 35_000_000L)

        val result = PopulationCellMapper.mapToSnapshots(cells)

        // Verify that the key type matches what DefaultPopulationComponent.buildContent uses:
        //   data.snapshots[activeCountry to activeYear]
        val key: Pair<String, Int> = "FR" to 2022
        assertNotNull(result[key], "Key type must be Pair<String,Int> — 'FR' to 2022")
    }

    // ------------------------------------------------------------------------------------
    // 13. mapToSnapshots: cohorts sorted youngest-to-oldest (Y_LT5 first, Y_GE85 last)
    // ------------------------------------------------------------------------------------

    @Test
    fun mapToSnapshots_cohortsOrderedYoungestToOldest() {
        val cohorts = syntheticCohorts()
        val cells = fullCohortCells("DE", "2023", cohorts, 41_000_000L, 42_000_000L)

        val result = PopulationCellMapper.mapToSnapshots(cells)
        val snapshot = result["DE" to 2023]!!

        assertEquals("Y_LT5", snapshot.cohorts.first().ageCode, "First cohort must be Y_LT5 (youngest)")
        assertEquals("Y_GE85", snapshot.cohorts.last().ageCode, "Last cohort must be Y_GE85 (oldest)")
    }

    // ------------------------------------------------------------------------------------
    // 14. mapToSnapshots: totalMale/totalFemale from age=TOTAL cells, not cohort sum
    // ------------------------------------------------------------------------------------

    @Test
    fun mapToSnapshots_totals_preferAgeTotal() {
        val cohorts = syntheticCohorts()
        // Deliberate mismatch: TOTAL rows say 41M/42M, cohort sum would differ.
        val cells = fullCohortCells("DE", "2023", cohorts, 41_000_000L, 42_000_000L)

        val result = PopulationCellMapper.mapToSnapshots(cells)
        val snapshot = result["DE" to 2023]!!

        assertEquals(41_000_000L, snapshot.totalMale, "totalMale must come from age=TOTAL sex=M")
        assertEquals(42_000_000L, snapshot.totalFemale, "totalFemale must come from age=TOTAL sex=F")
        assertEquals(83_000_000L, snapshot.total, "total must come from age=TOTAL sex=T")
    }

    // ------------------------------------------------------------------------------------
    // 15. mapToSnapshots: cells with null value are skipped (no zero-filled cohorts)
    // ------------------------------------------------------------------------------------

    @Test
    fun mapToSnapshots_nullValueCells_cohortSkipped() {
        val cells = listOf(
            // TOTAL row
            cell("PL", "2020", "T", "TOTAL", 38_000_000.0),
            cell("PL", "2020", "M", "TOTAL", 18_000_000.0),
            cell("PL", "2020", "F", "TOTAL", 20_000_000.0),
            // One cohort with non-null values
            cell("PL", "2020", "M", "Y_LT5", 900_000.0),
            cell("PL", "2020", "F", "Y_LT5", 850_000.0),
            // Another cohort with null value — should not produce a cohort entry
            cell("PL", "2020", "M", "Y5-9", null),
            cell("PL", "2020", "F", "Y5-9", null),
        )

        val result = PopulationCellMapper.mapToSnapshots(cells)
        val snapshot = result["PL" to 2020]

        assertNotNull(snapshot)
        // Y_LT5 produced a cohort; Y5-9 has null values so no bucket entry → only 1 cohort
        assertEquals(1, snapshot.cohorts.size, "Null-value cohort cells must not appear in the snapshot")
        assertEquals("Y_LT5", snapshot.cohorts[0].ageCode)
    }

    // ------------------------------------------------------------------------------------
    // 16. mapToSnapshots: multiple countries × multiple years produce distinct keys
    // ------------------------------------------------------------------------------------

    @Test
    fun mapToSnapshots_multipleCountriesAndYears_distinctKeys() {
        val cohorts = syntheticCohorts()
        val cells = buildList {
            addAll(fullCohortCells("DE", "2022", cohorts, 41_000_000L, 42_000_000L))
            addAll(fullCohortCells("DE", "2023", cohorts, 41_100_000L, 42_100_000L))
            addAll(fullCohortCells("FR", "2022", cohorts, 32_000_000L, 35_000_000L))
        }

        val result = PopulationCellMapper.mapToSnapshots(cells)

        assertEquals(3, result.size, "Each (country, year) combination must be a distinct key")
        assertNotNull(result["DE" to 2022])
        assertNotNull(result["DE" to 2023])
        assertNotNull(result["FR" to 2022])
    }
}
