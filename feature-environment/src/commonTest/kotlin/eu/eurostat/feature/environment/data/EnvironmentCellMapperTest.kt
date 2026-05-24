package eu.eurostat.feature.environment.data

import eu.eurostat.core.jsonstat.JsonStatCell
import eu.eurostat.feature.environment.domain.EnvSector
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [EnvironmentCellMapper.buildTimeSeries].
 *
 * Three input streams are mapped and merged:
 *  - GHG cells (env_air_gge): dimensions geo, time, src_crf, airpol, unit
 *  - Energy cells (nrg_bal_c): dimensions geo, time, nrg_bal, siec, unit
 *  - SDG cells (sdg_13_10): dimensions geo, time, unit
 *
 * GHG sector codes: TOTX4_MEMO → Total, CRF1A3 → Transport, CRF1A2 → Industry
 * Energy sector codes: FC_E → Total, FC_TRA_E → Transport, FC_IND_E → Industry
 * SDG points have sector = null.
 */
class EnvironmentCellMapperTest {

    // ------------------------------------------------------------------------------------
    // Helpers to build JsonStatCell instances for each dataset type
    // ------------------------------------------------------------------------------------

    private fun ghgCell(
        geo: String,
        time: String,
        srcCrf: String,
        value: Double?,
        geoLabel: String = geo,
    ) = JsonStatCell(
        dimensions = mapOf(
            "geo" to geo,
            "time" to time,
            "src_crf" to srcCrf,
            "airpol" to "GHG",
            "unit" to "MIO_T",
        ),
        dimensionLabels = mapOf("geo" to geoLabel),
        value = value,
    )

    private fun energyCell(
        geo: String,
        time: String,
        nrgBal: String,
        value: Double?,
        geoLabel: String = geo,
    ) = JsonStatCell(
        dimensions = mapOf(
            "geo" to geo,
            "time" to time,
            "nrg_bal" to nrgBal,
            "siec" to "TOTAL",
            "unit" to "KTOE",
        ),
        dimensionLabels = mapOf("geo" to geoLabel),
        value = value,
    )

    private fun sdgCell(
        geo: String,
        time: String,
        value: Double?,
        geoLabel: String = geo,
    ) = JsonStatCell(
        dimensions = mapOf(
            "geo" to geo,
            "time" to time,
            "unit" to "I90",
        ),
        dimensionLabels = mapOf("geo" to geoLabel),
        value = value,
    )

    // ------------------------------------------------------------------------------------
    // 1. Happy path: all three datasets have data for one country, one year
    // ------------------------------------------------------------------------------------

    @Test
    fun buildTimeSeries_happyPath_oneCountryOneYear_producesCorrectSeries() {
        val ghg = listOf(
            ghgCell("PL", "2020", "TOTX4_MEMO", 400.0),
            ghgCell("PL", "2020", "CRF1A3", 120.0),
            ghgCell("PL", "2020", "CRF1A2", 80.0),
        )
        val energy = listOf(
            energyCell("PL", "2020", "FC_E", 100_000.0),
            energyCell("PL", "2020", "FC_TRA_E", 30_000.0),
            energyCell("PL", "2020", "FC_IND_E", 25_000.0),
        )
        val sdg = listOf(sdgCell("PL", "2020", 92.5))

        val result = EnvironmentCellMapper.buildTimeSeries(ghg, energy, sdg)

        assertEquals(1, result.size)
        val ts = result[0]
        assertEquals("PL", ts.countryCode)

        // 3 sector points from GHG+Energy merge + 1 SDG point (sector=null)
        assertEquals(4, ts.points.size)

        val totalPoint = ts.points.first { it.sector == EnvSector.Total }
        assertEquals(400.0, totalPoint.ghgMtCo2eq)
        assertEquals(100_000.0, totalPoint.energyKtoe)
        assertNull(totalPoint.sdg13Index)

        val transportPoint = ts.points.first { it.sector == EnvSector.Transport }
        assertEquals(120.0, transportPoint.ghgMtCo2eq)
        assertEquals(30_000.0, transportPoint.energyKtoe)

        val industryPoint = ts.points.first { it.sector == EnvSector.Industry }
        assertEquals(80.0, industryPoint.ghgMtCo2eq)
        assertEquals(25_000.0, industryPoint.energyKtoe)

        val sdgPoint = ts.points.first { it.sector == null }
        assertEquals(92.5, sdgPoint.sdg13Index)
        assertNull(sdgPoint.ghgMtCo2eq)
        assertNull(sdgPoint.energyKtoe)
    }

    // ------------------------------------------------------------------------------------
    // 2. Two countries: each gets their own EnvironmentTimeSeries
    // ------------------------------------------------------------------------------------

    @Test
    fun buildTimeSeries_twoCountries_producesTwoSeries() {
        val ghg = listOf(
            ghgCell("PL", "2020", "TOTX4_MEMO", 400.0),
            ghgCell("DE", "2020", "TOTX4_MEMO", 800.0),
        )
        val energy = listOf(
            energyCell("PL", "2020", "FC_E", 100_000.0),
            energyCell("DE", "2020", "FC_E", 300_000.0),
        )
        val sdg = listOf(
            sdgCell("PL", "2020", 92.5),
            sdgCell("DE", "2020", 88.0),
        )

        val result = EnvironmentCellMapper.buildTimeSeries(ghg, energy, sdg)

        assertEquals(2, result.size)
        val codes = result.map { it.countryCode }
        assertTrue("PL" in codes)
        assertTrue("DE" in codes)
    }

    // ------------------------------------------------------------------------------------
    // 3. Country label is taken from dimensionLabels when available
    // ------------------------------------------------------------------------------------

    @Test
    fun buildTimeSeries_countryLabel_takenFromDimensionLabels() {
        val ghg = listOf(ghgCell("PL", "2020", "TOTX4_MEMO", 400.0, geoLabel = "Poland"))
        val energy = listOf(energyCell("PL", "2020", "FC_E", 100_000.0))

        val result = EnvironmentCellMapper.buildTimeSeries(ghg, energy, null)

        assertEquals("Poland", result[0].countryName)
    }

    // ------------------------------------------------------------------------------------
    // 4. Null value in GHG cell → ghgMtCo2eq is null on merged point
    // ------------------------------------------------------------------------------------

    @Test
    fun buildTimeSeries_nullGhgValue_ghgMtCo2eqIsNull() {
        val ghg = listOf(ghgCell("PL", "2020", "TOTX4_MEMO", null))
        val energy = listOf(energyCell("PL", "2020", "FC_E", 100_000.0))

        val result = EnvironmentCellMapper.buildTimeSeries(ghg, energy, null)

        val totalPoint = result[0].points.first { it.sector == EnvSector.Total }
        assertNull(totalPoint.ghgMtCo2eq, "Null GHG cell value must produce null ghgMtCo2eq")
        assertEquals(100_000.0, totalPoint.energyKtoe)
    }

    // ------------------------------------------------------------------------------------
    // 5. Null value in Energy cell → energyKtoe is null on merged point
    // ------------------------------------------------------------------------------------

    @Test
    fun buildTimeSeries_nullEnergyValue_energyKtoeIsNull() {
        val ghg = listOf(ghgCell("PL", "2020", "CRF1A3", 120.0))
        val energy = listOf(energyCell("PL", "2020", "FC_TRA_E", null))

        val result = EnvironmentCellMapper.buildTimeSeries(ghg, energy, null)

        val transportPoint = result[0].points.first { it.sector == EnvSector.Transport }
        assertEquals(120.0, transportPoint.ghgMtCo2eq)
        assertNull(transportPoint.energyKtoe, "Null energy cell value must produce null energyKtoe")
    }

    // ------------------------------------------------------------------------------------
    // 6. SDG null value → sdg13Index is null (point still emitted)
    // ------------------------------------------------------------------------------------

    @Test
    fun buildTimeSeries_nullSdgValue_sdg13IndexIsNull() {
        val ghg = listOf(ghgCell("PL", "2020", "TOTX4_MEMO", 400.0))
        val energy = listOf(energyCell("PL", "2020", "FC_E", 100_000.0))
        val sdg = listOf(sdgCell("PL", "2020", null))

        val result = EnvironmentCellMapper.buildTimeSeries(ghg, energy, sdg)

        val sdgPoint = result[0].points.firstOrNull { it.sector == null }
        assertNotNull(sdgPoint, "SDG cell with null value should still produce a data point")
        assertNull(sdgPoint!!.sdg13Index)
    }

    // ------------------------------------------------------------------------------------
    // 7. Unexpected GHG sector code → cell skipped (mapper ignores unknown src_crf codes)
    // ------------------------------------------------------------------------------------

    @Test
    fun buildTimeSeries_unknownGhgSectorCode_cellSkipped() {
        val ghg = listOf(
            ghgCell("PL", "2020", "TOTX4_MEMO", 400.0),
            // Unknown src_crf code that is not in the mapping
            JsonStatCell(
                dimensions = mapOf("geo" to "PL", "time" to "2020", "src_crf" to "CRF_UNKNOWN"),
                value = 999.0,
            ),
        )
        val energy = listOf(energyCell("PL", "2020", "FC_E", 100_000.0))

        val result = EnvironmentCellMapper.buildTimeSeries(ghg, energy, null)

        // Only the known sector Total should appear (from GHG+Energy merge)
        val sectoredPoints = result[0].points.filter { it.sector != null }
        assertEquals(1, sectoredPoints.size, "Unknown sector code must be skipped")
        assertEquals(EnvSector.Total, sectoredPoints[0].sector)
    }

    // ------------------------------------------------------------------------------------
    // 8. Unexpected Energy sector code → cell skipped
    // ------------------------------------------------------------------------------------

    @Test
    fun buildTimeSeries_unknownEnergySectorCode_cellSkipped() {
        val ghg = listOf(ghgCell("PL", "2020", "TOTX4_MEMO", 400.0))
        val energy = listOf(
            energyCell("PL", "2020", "FC_E", 100_000.0),
            // Unknown nrg_bal code
            JsonStatCell(
                dimensions = mapOf("geo" to "PL", "time" to "2020", "nrg_bal" to "UNKNOWN_BAL"),
                value = 9_999.0,
            ),
        )

        val result = EnvironmentCellMapper.buildTimeSeries(ghg, energy, null)

        val sectoredPoints = result[0].points.filter { it.sector != null }
        assertEquals(1, sectoredPoints.size, "Unknown energy sector code must be skipped")
        assertEquals(EnvSector.Total, sectoredPoints[0].sector)
    }

    // ------------------------------------------------------------------------------------
    // 9. Missing geo dimension → cell skipped
    // ------------------------------------------------------------------------------------

    @Test
    fun buildTimeSeries_missingGeoDimension_cellSkipped() {
        val ghg = listOf(
            // Valid cell
            ghgCell("PL", "2020", "TOTX4_MEMO", 400.0),
            // Cell without geo
            JsonStatCell(
                dimensions = mapOf("time" to "2020", "src_crf" to "TOTX4_MEMO"),
                value = 999.0,
            ),
        )
        val energy = listOf(energyCell("PL", "2020", "FC_E", 100_000.0))

        val result = EnvironmentCellMapper.buildTimeSeries(ghg, energy, null)

        assertEquals(1, result.size)
        assertEquals("PL", result[0].countryCode)
    }

    // ------------------------------------------------------------------------------------
    // 10. Invalid time (non-integer) → cell skipped
    // ------------------------------------------------------------------------------------

    @Test
    fun buildTimeSeries_invalidTimeString_cellSkipped() {
        val ghg = listOf(
            ghgCell("PL", "2020", "TOTX4_MEMO", 400.0),
            // invalid time
            JsonStatCell(
                dimensions = mapOf("geo" to "PL", "time" to "not-a-year", "src_crf" to "TOTX4_MEMO"),
                value = 999.0,
            ),
        )
        val energy = listOf(energyCell("PL", "2020", "FC_E", 100_000.0))

        val result = EnvironmentCellMapper.buildTimeSeries(ghg, energy, null)

        // Only 2020 point present, not the invalid time
        val sectoredPoints = result[0].points.filter { it.sector != null }
        assertEquals(1, sectoredPoints.size)
        assertEquals(2020, sectoredPoints[0].year)
    }

    // ------------------------------------------------------------------------------------
    // 11. GHG-only year (no energy for that year): point carries ghg, energy=null
    //     The mapper uses a union of GHG and Energy keys so both single-source points appear.
    // ------------------------------------------------------------------------------------

    @Test
    fun buildTimeSeries_ghgOnlyYear_energyIsNull() {
        val ghg = listOf(ghgCell("PL", "2021", "TOTX4_MEMO", 390.0))
        // Energy only has 2020 data
        val energy = listOf(energyCell("PL", "2020", "FC_E", 100_000.0))

        val result = EnvironmentCellMapper.buildTimeSeries(ghg, energy, null)

        val ghg2021 = result[0].points.firstOrNull { it.year == 2021 && it.sector == EnvSector.Total }
        assertNotNull(ghg2021, "GHG-only year 2021 must produce a point with sector=Total")
        assertEquals(390.0, ghg2021!!.ghgMtCo2eq)
        assertNull(ghg2021.energyKtoe, "Energy-less point must have null energyKtoe")

        val energy2020 = result[0].points.firstOrNull { it.year == 2020 && it.sector == EnvSector.Total }
        assertNotNull(energy2020, "Energy-only year 2020 must produce a point with sector=Total")
        assertEquals(100_000.0, energy2020!!.energyKtoe)
        assertNull(energy2020.ghgMtCo2eq, "GHG-less point must have null ghgMtCo2eq")
    }

    // ------------------------------------------------------------------------------------
    // 12. SDG null input (null passed as parameter) → treated as empty, no SDG points
    // ------------------------------------------------------------------------------------

    @Test
    fun buildTimeSeries_nullSdgInput_noSdgPoints() {
        val ghg = listOf(ghgCell("PL", "2020", "TOTX4_MEMO", 400.0))
        val energy = listOf(energyCell("PL", "2020", "FC_E", 100_000.0))

        val result = EnvironmentCellMapper.buildTimeSeries(ghg, energy, sdgCells = null)

        val sdgPoints = result[0].points.filter { it.sector == null }
        assertTrue(sdgPoints.isEmpty(), "Null SDG input should produce no SDG data points")
    }

    // ------------------------------------------------------------------------------------
    // 13. Empty inputs → empty result
    // ------------------------------------------------------------------------------------

    @Test
    fun buildTimeSeries_emptyInputs_returnsEmpty() {
        val result = EnvironmentCellMapper.buildTimeSeries(emptyList(), emptyList(), emptyList())
        assertTrue(result.isEmpty())
    }

    // ------------------------------------------------------------------------------------
    // 14. Points sorted by (sector ordinal, year): Total < Transport < Industry < null (SDG last)
    // ------------------------------------------------------------------------------------

    @Test
    fun buildTimeSeries_pointsSortedBySectorOrdinalThenYear() {
        val ghg = listOf(
            ghgCell("PL", "2021", "CRF1A3", 110.0),
            ghgCell("PL", "2020", "TOTX4_MEMO", 400.0),
            ghgCell("PL", "2020", "CRF1A2", 80.0),
            ghgCell("PL", "2020", "CRF1A3", 120.0),
            ghgCell("PL", "2021", "TOTX4_MEMO", 390.0),
        )
        val energy = listOf(
            energyCell("PL", "2020", "FC_E", 100_000.0),
            energyCell("PL", "2021", "FC_E", 95_000.0),
        )
        val sdg = listOf(
            sdgCell("PL", "2020", 92.5),
            sdgCell("PL", "2021", 91.0),
        )

        val result = EnvironmentCellMapper.buildTimeSeries(ghg, energy, sdg)
        val points = result[0].points

        // All non-null sectors appear before null-sector (SDG) points
        val sectoredPoints = points.filter { it.sector != null }
        val sdgPoints = points.filter { it.sector == null }

        // The last sector point should appear before the first SDG point
        val lastSectorIdx = points.indexOfLast { it.sector != null }
        val firstSdgIdx = points.indexOfFirst { it.sector == null }
        if (sdgPoints.isNotEmpty() && sectoredPoints.isNotEmpty()) {
            assertTrue(
                lastSectorIdx < firstSdgIdx,
                "Sector points must appear before SDG points (sector=null)"
            )
        }

        // Within sectored points, Total < Transport < Industry by ordinal
        val sectorOrders = sectoredPoints.map { it.sector?.ordinal ?: Int.MAX_VALUE }
        assertEquals(sectorOrders.sorted(), sectorOrders, "Points must be sorted by sector ordinal")
    }

    // ------------------------------------------------------------------------------------
    // 15. Series sorted alphabetically by countryCode
    // ------------------------------------------------------------------------------------

    @Test
    fun buildTimeSeries_seriesSortedByCountryCode() {
        val ghg = listOf(
            ghgCell("PL", "2020", "TOTX4_MEMO", 400.0),
            ghgCell("AT", "2020", "TOTX4_MEMO", 60.0),
            ghgCell("DE", "2020", "TOTX4_MEMO", 800.0),
        )
        val energy = emptyList<JsonStatCell>()

        val result = EnvironmentCellMapper.buildTimeSeries(ghg, energy, null)

        assertEquals(listOf("AT", "DE", "PL"), result.map { it.countryCode })
    }
}
