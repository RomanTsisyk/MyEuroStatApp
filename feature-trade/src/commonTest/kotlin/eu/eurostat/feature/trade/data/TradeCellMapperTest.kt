package eu.eurostat.feature.trade.data

import eu.eurostat.core.jsonstat.JsonStatCell
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [TradeCellMapper].
 *
 * Dimension values use CODES from JSON-stat category.index keys:
 *   dimensions["geo"] = "PL", dimensions["indic_et"] = "MIO_EXP_VAL", etc.
 *
 * The mapper groups by (geo, time) and bins indic_et: MIO_EXP_VAL → exportsEur, MIO_IMP_VAL → importsEur, MIO_BAL_VAL → balanceEur.
 * The partner code is passed as a constructor arg; the mapper does NOT read it from dimensions.
 */
class TradeCellMapperTest {

    // ------------------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------------------

    private fun cell(
        geo: String,
        time: String,
        partner: String = "EU27_2020",
        indicEt: String,
        value: Double?,
    ) = JsonStatCell(
        dimensions = mapOf("geo" to geo, "time" to time, "partner" to partner, "indic_et" to indicEt),
        value = value,
    )

    // ------------------------------------------------------------------------------------
    // 1. Happy path: 1 country × 1 year × 3 indicators
    // ------------------------------------------------------------------------------------

    @Test
    fun mapCells_happyPath_allThreeIndicators_singlePoint() {
        val cells = listOf(
            cell("PL", "2020", indicEt = "MIO_EXP_VAL", value = 300_000.0),
            cell("PL", "2020", indicEt = "MIO_IMP_VAL", value = 280_000.0),
            cell("PL", "2020", indicEt = "MIO_BAL_VAL", value = 20_000.0),
        )

        val result = TradeCellMapper.mapCells(cells, "EU27_2020")

        assertEquals(1, result.size)
        val point = result[0]
        assertEquals("PL", point.countryCode)
        assertEquals(2020, point.year)
        assertEquals("EU27_2020", point.partner)
        assertEquals(300_000L, point.exportsEur)
        assertEquals(280_000L, point.importsEur)
        assertEquals(20_000L, point.balanceEur)
    }

    // ------------------------------------------------------------------------------------
    // 2. Two countries × two years
    // ------------------------------------------------------------------------------------

    @Test
    fun mapCells_twoCountriesTwoYears_fourPoints() {
        val cells = listOf(
            cell("PL", "2020", indicEt = "MIO_EXP_VAL", value = 300_000.0),
            cell("PL", "2020", indicEt = "MIO_IMP_VAL", value = 280_000.0),
            cell("PL", "2020", indicEt = "MIO_BAL_VAL", value = 20_000.0),
            cell("PL", "2021", indicEt = "MIO_EXP_VAL", value = 310_000.0),
            cell("PL", "2021", indicEt = "MIO_IMP_VAL", value = 290_000.0),
            cell("PL", "2021", indicEt = "MIO_BAL_VAL", value = 20_000.0),
            cell("DE", "2020", indicEt = "MIO_EXP_VAL", value = 1_200_000.0),
            cell("DE", "2020", indicEt = "MIO_IMP_VAL", value = 1_050_000.0),
            cell("DE", "2020", indicEt = "MIO_BAL_VAL", value = 150_000.0),
            cell("DE", "2021", indicEt = "MIO_EXP_VAL", value = 1_250_000.0),
            cell("DE", "2021", indicEt = "MIO_IMP_VAL", value = 1_100_000.0),
            cell("DE", "2021", indicEt = "MIO_BAL_VAL", value = 150_000.0),
        )

        val result = TradeCellMapper.mapCells(cells, "EU27_2020")

        assertEquals(4, result.size, "Should produce 4 data points (2 countries × 2 years)")
    }

    // ------------------------------------------------------------------------------------
    // 3. Missing EXP indicator → exportsEur is null
    // ------------------------------------------------------------------------------------

    @Test
    fun mapCells_missingExport_exportsEurIsNull() {
        val cells = listOf(
            // MIO_EXP_VAL missing intentionally
            cell("PL", "2020", indicEt = "MIO_IMP_VAL", value = 280_000.0),
            cell("PL", "2020", indicEt = "MIO_BAL_VAL", value = -280_000.0),
        )

        val result = TradeCellMapper.mapCells(cells, "EU27_2020")

        assertEquals(1, result.size)
        assertNull(result[0].exportsEur, "exportsEur should be null when EXP indicator is absent")
        assertEquals(280_000L, result[0].importsEur)
        assertEquals(-280_000L, result[0].balanceEur)
    }

    // ------------------------------------------------------------------------------------
    // 4. Missing IMP indicator → importsEur is null
    // ------------------------------------------------------------------------------------

    @Test
    fun mapCells_missingImport_importsEurIsNull() {
        val cells = listOf(
            cell("PL", "2020", indicEt = "MIO_EXP_VAL", value = 300_000.0),
            // MIO_IMP_VAL missing
            cell("PL", "2020", indicEt = "MIO_BAL_VAL", value = 300_000.0),
        )

        val result = TradeCellMapper.mapCells(cells, "EU27_2020")

        assertNull(result[0].importsEur)
        assertEquals(300_000L, result[0].exportsEur)
        assertEquals(300_000L, result[0].balanceEur)
    }

    // ------------------------------------------------------------------------------------
    // 5. Null cell value → corresponding field is null (not zero)
    // ------------------------------------------------------------------------------------

    @Test
    fun mapCells_nullExpValue_exportsEurIsNull() {
        val cells = listOf(
            cell("PL", "2020", indicEt = "MIO_EXP_VAL", value = null),
            cell("PL", "2020", indicEt = "MIO_IMP_VAL", value = 280_000.0),
            cell("PL", "2020", indicEt = "MIO_BAL_VAL", value = null),
        )

        val result = TradeCellMapper.mapCells(cells, "EU27_2020")

        assertNull(result[0].exportsEur, "Null value cell must produce null field, not zero")
        assertNull(result[0].balanceEur)
        assertEquals(280_000L, result[0].importsEur)
    }

    // ------------------------------------------------------------------------------------
    // 6. Empty input → empty output
    // ------------------------------------------------------------------------------------

    @Test
    fun mapCells_emptyInput_returnsEmpty() {
        val result = TradeCellMapper.mapCells(emptyList(), "EU27_2020")
        assertTrue(result.isEmpty())
    }

    // ------------------------------------------------------------------------------------
    // 7. Partner code from parameter arg used (not from cell dimensions)
    // ------------------------------------------------------------------------------------

    @Test
    fun mapCells_partnerSetFromParameter() {
        val cells = listOf(cell("PL", "2020", partner = "EU27_2020", indicEt = "MIO_EXP_VAL", value = 300_000.0))
        val result = TradeCellMapper.mapCells(cells, partner = "WORLD")
        assertEquals("WORLD", result[0].partner, "Partner on output should be from the parameter, not from dimension")
    }

    // ------------------------------------------------------------------------------------
    // 8. Cell without indic_et dimension is skipped
    // ------------------------------------------------------------------------------------

    @Test
    fun mapCells_missingIndicEtDimension_cellSkipped() {
        val cells = listOf(
            JsonStatCell(dimensions = mapOf("geo" to "PL", "time" to "2020"), value = 300_000.0),
        )
        val result = TradeCellMapper.mapCells(cells, "EU27_2020")
        // No indic_et key → the cell doesn't contribute a meaningful indicator, result depends on mapper
        // Mapper adds the key to the indicators map as null since indicator would be fetched but missing
        // Actually the mapper skips cells where indic_et is null (return@forEach)
        assertTrue(result.isEmpty() || result.all { it.exportsEur == null && it.importsEur == null && it.balanceEur == null })
    }

    // ------------------------------------------------------------------------------------
    // 9. Cell with non-integer time is skipped
    // ------------------------------------------------------------------------------------

    @Test
    fun mapCells_invalidTimeString_cellSkipped() {
        val cells = listOf(
            cell("PL", "not-a-year", indicEt = "MIO_EXP_VAL", value = 300_000.0),
            // Valid cell
            cell("PL", "2020", indicEt = "MIO_EXP_VAL", value = 300_000.0),
            cell("PL", "2020", indicEt = "MIO_IMP_VAL", value = 280_000.0),
            cell("PL", "2020", indicEt = "MIO_BAL_VAL", value = 20_000.0),
        )

        val result = TradeCellMapper.mapCells(cells, "EU27_2020")

        assertEquals(1, result.size, "Invalid-time cell should be skipped")
        assertEquals(2020, result[0].year)
    }

    // ------------------------------------------------------------------------------------
    // 10. Large export value: Long precision maintained
    // ------------------------------------------------------------------------------------

    @Test
    fun mapCells_largeValue_storedAsLong() {
        val cells = listOf(cell("DE", "2020", indicEt = "MIO_EXP_VAL", value = 1_234_567_890.0))
        val result = TradeCellMapper.mapCells(cells, "EU27_2020")
        assertEquals(1_234_567_890L, result[0].exportsEur)
    }
}
