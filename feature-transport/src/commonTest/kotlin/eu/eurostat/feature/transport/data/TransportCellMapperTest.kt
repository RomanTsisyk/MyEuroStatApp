package eu.eurostat.feature.transport.data

import eu.eurostat.core.jsonstat.JsonStatCell
import eu.eurostat.feature.transport.domain.TransportMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TransportCellMapperTest {

    // ---------------------------------------------------------------------------
    // mapRoadCells
    // ---------------------------------------------------------------------------

    @Test
    fun mapRoadCells_sets_roadPassengers_and_nullifies_others() {
        val cells = listOf(
            JsonStatCell(mapOf("geo" to "PL", "time" to "2020"), value = 5_000_000.0),
            JsonStatCell(mapOf("geo" to "DE", "time" to "2021"), value = 12_000_000.0),
        )
        val result = TransportCellMapper.mapRoadCells(cells)
        assertEquals(2, result.size)

        // Road values are in THS_PAS, mapper scales ×1000 to raw passengers
        val pl = result.first { it.countryCode == "PL" }
        assertEquals(TransportMode.ROAD, pl.mode)
        assertEquals(5_000_000_000L, pl.roadPassengers)
        assertNull(pl.airPassengers)
        assertNull(pl.seaPassengers)
        assertEquals(2020, pl.year)

        val de = result.first { it.countryCode == "DE" }
        assertEquals(12_000_000_000L, de.roadPassengers)
        assertNull(de.airPassengers)
    }

    @Test
    fun mapRoadCells_null_value_produces_null_roadPassengers() {
        val cells = listOf(
            JsonStatCell(mapOf("geo" to "FR", "time" to "2022"), value = null),
        )
        val result = TransportCellMapper.mapRoadCells(cells)
        assertEquals(1, result.size)
        assertNull(result[0].roadPassengers)
        assertNull(result[0].airPassengers)
        assertNull(result[0].seaPassengers)
    }

    @Test
    fun mapRoadCells_skips_cells_missing_geo() {
        val cells = listOf(
            JsonStatCell(mapOf("time" to "2020"), value = 1000.0),
        )
        assertTrue(TransportCellMapper.mapRoadCells(cells).isEmpty())
    }

    @Test
    fun mapRoadCells_skips_cells_with_non_integer_time() {
        val cells = listOf(
            JsonStatCell(mapOf("geo" to "PL", "time" to "Q1-2020"), value = 1000.0),
        )
        assertTrue(TransportCellMapper.mapRoadCells(cells).isEmpty())
    }

    @Test
    fun mapRoadCells_empty_input_returns_empty() {
        assertTrue(TransportCellMapper.mapRoadCells(emptyList()).isEmpty())
    }

    // ---------------------------------------------------------------------------
    // mapAirCells
    // ---------------------------------------------------------------------------

    @Test
    fun mapAirCells_sets_airPassengers_and_nullifies_others() {
        val cells = listOf(
            JsonStatCell(mapOf("geo" to "PL", "time" to "2020"), value = 8_500_000.0),
        )
        val result = TransportCellMapper.mapAirCells(cells)
        assertEquals(1, result.size)
        assertEquals(TransportMode.AIR, result[0].mode)
        assertEquals(8_500_000L, result[0].airPassengers)
        assertNull(result[0].roadPassengers)
        assertNull(result[0].seaPassengers)
    }

    // ---------------------------------------------------------------------------
    // mapSeaCells
    // ---------------------------------------------------------------------------

    @Test
    fun mapSeaCells_sets_seaPassengers_and_nullifies_others() {
        val cells = listOf(
            JsonStatCell(mapOf("geo" to "GR", "time" to "2019"), value = 3_200_000.0),
        )
        val result = TransportCellMapper.mapSeaCells(cells)
        assertEquals(1, result.size)
        assertEquals(TransportMode.SEA, result[0].mode)
        assertEquals(3_200_000L, result[0].seaPassengers)
        assertNull(result[0].roadPassengers)
        assertNull(result[0].airPassengers)
    }

    // ---------------------------------------------------------------------------
    // mergeAll
    // ---------------------------------------------------------------------------

    @Test
    fun mergeAll_combines_three_lists_by_country_and_year() {
        val roadCells = listOf(JsonStatCell(dimensions = mapOf("geo" to "PL", "time" to "2020"), value = 5_000_000.0))
        val airCells  = listOf(JsonStatCell(dimensions = mapOf("geo" to "PL", "time" to "2020"), value = 8_500_000.0))
        val seaCells  = listOf(JsonStatCell(dimensions = mapOf("geo" to "PL", "time" to "2020"), value = 2_000_000.0))

        val road = TransportCellMapper.mapRoadCells(roadCells)
        val air  = TransportCellMapper.mapAirCells(airCells)
        val sea  = TransportCellMapper.mapSeaCells(seaCells)

        val merged = TransportCellMapper.mergeAll(road, air, sea)
        assertEquals(1, merged.size)
        val pt = merged[0]
        assertEquals("PL", pt.countryCode)
        assertEquals(2020, pt.year)
        assertEquals(TransportMode.ALL, pt.mode)
        // Road scales ×1000; air and sea stay raw
        assertEquals(5_000_000_000L, pt.roadPassengers)
        assertEquals(8_500_000L, pt.airPassengers)
        assertEquals(2_000_000L, pt.seaPassengers)
    }

    @Test
    fun mergeAll_handles_partial_inputs_with_union_and_nulls() {
        // Road has PL-2020 and DE-2021; air has only PL-2020; sea is empty
        val roadCells = listOf(
            JsonStatCell(dimensions = mapOf("geo" to "PL", "time" to "2020"), value = 5_000_000.0),
            JsonStatCell(dimensions = mapOf("geo" to "DE", "time" to "2021"), value = 12_000_000.0),
        )
        val airCells = listOf(
            JsonStatCell(dimensions = mapOf("geo" to "PL", "time" to "2020"), value = 8_500_000.0),
        )

        val road = TransportCellMapper.mapRoadCells(roadCells)
        val air  = TransportCellMapper.mapAirCells(airCells)
        val sea  = TransportCellMapper.mapSeaCells(emptyList())

        val merged = TransportCellMapper.mergeAll(road, air, sea)
        assertEquals(2, merged.size)

        val pl = merged.first { it.countryCode == "PL" }
        assertEquals(5_000_000_000L, pl.roadPassengers)
        assertEquals(8_500_000L, pl.airPassengers)
        assertNull(pl.seaPassengers)

        val de = merged.first { it.countryCode == "DE" }
        assertEquals(12_000_000_000L, de.roadPassengers)
        assertNull(de.airPassengers)
        assertNull(de.seaPassengers)
    }

    @Test
    fun mergeAll_all_empty_returns_empty() {
        val merged = TransportCellMapper.mergeAll(emptyList(), emptyList(), emptyList())
        assertTrue(merged.isEmpty())
    }

    @Test
    fun mergeAll_multiple_countries_and_years() {
        val countries = listOf("PL", "DE", "FR")
        val years     = listOf("2020", "2021", "2022")

        val roadCells = countries.flatMap { c -> years.map { y ->
            JsonStatCell(dimensions = mapOf("geo" to c, "time" to y), value = 1_000.0)
        }}
        val airCells = countries.flatMap { c -> years.map { y ->
            JsonStatCell(dimensions = mapOf("geo" to c, "time" to y), value = 2_000.0)
        }}
        val seaCells = emptyList<JsonStatCell>()

        val merged = TransportCellMapper.mergeAll(
            TransportCellMapper.mapRoadCells(roadCells),
            TransportCellMapper.mapAirCells(airCells),
            TransportCellMapper.mapSeaCells(seaCells),
        )
        // 3 countries × 3 years = 9 unique (country, year) pairs
        assertEquals(9, merged.size)
        assertTrue(merged.all { it.mode == TransportMode.ALL })
        assertTrue(merged.all { it.roadPassengers == 1_000_000L }, "road values scaled ×1000")
        assertTrue(merged.all { it.airPassengers == 2_000L })
        assertTrue(merged.all { it.seaPassengers == null })
    }
}
