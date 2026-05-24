package eu.eurostat.core.jsonstat

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class JsonStatParserTest {

    private val parser = JsonStatParser()
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun multipliers_for_three_dimensions() {
        val out = parser.computeMultipliers(listOf(27, 24, 3))
        assertEquals(72, out[0])
        assertEquals(3, out[1])
        assertEquals(1, out[2])
    }

    @Test
    fun multipliers_for_single_dimension() {
        val out = parser.computeMultipliers(listOf(5))
        assertEquals(1, out[0])
    }

    @Test
    fun parses_three_dimension_population_response() {
        val payload = """
            {
              "id": ["geo", "time", "sex"],
              "size": [2, 2, 3],
              "dimension": {
                "geo":  { "category": { "index": { "PL": 0, "DE": 1 } } },
                "time": { "category": { "index": { "2022": 0, "2023": 1 } } },
                "sex":  { "category": { "index": { "T": 0, "M": 1, "F": 2 } } }
              },
              "value": {
                "0": 37800000, "1": 18300000, "2": 19500000,
                "3": 37500000, "4": 18200000, "5": 19300000,
                "6": 84000000, "7": 41200000, "8": 42800000,
                "9": 83800000, "10": 41100000, "11": 42700000
              }
            }
        """.trimIndent()

        val cells = parser.parse(json.decodeFromString(payload))

        assertEquals(12, cells.size)

        // Index 0 = (geo=0, time=0, sex=0) => PL, 2022, T = 37800000
        val first = cells.single { it.dimensions["geo"] == "PL" && it.dimensions["time"] == "2022" && it.dimensions["sex"] == "T" }
        assertEquals(37800000.0, first.value)

        // Index 11 = (1, 1, 2) => DE, 2023, F = 42700000
        val last = cells.single { it.dimensions["geo"] == "DE" && it.dimensions["time"] == "2023" && it.dimensions["sex"] == "F" }
        assertEquals(42700000.0, last.value)
    }

    @Test
    fun preserves_null_values_for_missing_observations() {
        val payload = """
            {
              "id": ["geo", "time"],
              "size": [1, 2],
              "dimension": {
                "geo":  { "category": { "index": { "PL": 0 } } },
                "time": { "category": { "index": { "2022": 0, "2023": 1 } } }
              },
              "value": { "0": null, "1": 37500000 }
            }
        """.trimIndent()

        val cells = parser.parse(json.decodeFromString(payload))

        assertNull(cells.first { it.dimensions["time"] == "2022" }.value)
        assertEquals(37500000.0, cells.first { it.dimensions["time"] == "2023" }.value)
    }

    @Test
    fun skips_non_integer_keys_in_value_map() {
        val payload = """
            {
              "id": ["geo"],
              "size": [2],
              "dimension": {
                "geo": { "category": { "index": { "PL": 0, "DE": 1 } } }
              },
              "value": { "0": 37500000, "bad_key": 99999, "1": 83800000 }
            }
        """.trimIndent()

        val cells = parser.parse(json.decodeFromString(payload))

        // non-integer key "bad_key" is skipped; only entries "0" and "1" are parsed
        assertEquals(2, cells.size)
        assertEquals(37500000.0, cells.first { it.dimensions["geo"] == "PL" }.value)
        assertEquals(83800000.0, cells.first { it.dimensions["geo"] == "DE" }.value)
    }

    @Test
    fun computeMultipliers_throws_on_int_overflow() {
        // The multiplier for the first dimension is 50_000 * 50_000 = 2_500_000_000 > Int.MAX_VALUE.
        // The require() fires when computing out[0], before the product overflows Int.
        assertFailsWith<IllegalArgumentException> {
            parser.computeMultipliers(listOf(3, 50_000, 50_000))
        }
    }

    @Test
    fun handles_dense_array_value_format() {
        val payload = """
            {
              "id": ["geo"],
              "size": [3],
              "dimension": {
                "geo": { "category": { "index": { "PL": 0, "DE": 1, "FR": 2 } } }
              },
              "value": [37500000, 83800000, 67800000]
            }
        """.trimIndent()

        val cells = parser.parse(json.decodeFromString(payload))

        assertEquals(3, cells.size)
        assertEquals(67800000.0, cells.first { it.dimensions["geo"] == "FR" }.value)
    }
}
