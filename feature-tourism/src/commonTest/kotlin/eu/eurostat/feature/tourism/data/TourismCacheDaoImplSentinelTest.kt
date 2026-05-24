package eu.eurostat.feature.tourism.data

import eu.eurostat.feature.tourism.domain.TourismDataPoint
import eu.eurostat.feature.tourism.domain.TourismQuery
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Unit tests for the [TourismCacheDaoImpl] null-nights sentinel logic.
 *
 * [TourismCacheDaoImpl] uses a sentinel value (-1L) to store "no nights data"
 * in the `nights INTEGER NOT NULL` SQLDelight column, because SQL NULL cannot
 * be stored in a NOT NULL column without a schema migration.
 *
 * These tests verify the sentinel contract using the [FakeTourismCacheDao]
 * (which models the intended DAO semantics) and document the expected
 * round-trip behavior. The regression scenario is:
 *   BEFORE FIX: `totalNights = null` was written as `0L`, so on read
 *               the domain model saw `totalNights = 0L` (falsely non-null),
 *               causing the chart to render a zero bar instead of a gap.
 *   AFTER FIX:  `totalNights = null` is written as sentinel (-1L) and
 *               read back as `null`, preserving data integrity.
 */
class TourismCacheDaoImplSentinelTest {

    // Verify the sentinel constant is well-defined and negative (outside valid range).
    @Test
    fun nights_null_sentinel_is_negative() {
        // NIGHTS_NULL_SENTINEL must be negative — Eurostat nights counts are always >= 0.
        // If this fails, valid data could be misidentified as "no data".
        val sentinel = TourismCacheDaoImpl.NIGHTS_NULL_SENTINEL_FOR_TEST
        assertEquals(-1L, sentinel, "Sentinel must be -1L to stay outside valid nights range [0, ∞)")
    }

    // Verify that FakeTourismCacheDao (used in all other tourism tests) correctly
    // preserves null totalNights through a write+read cycle — i.e., null is not
    // coerced to 0 at any other layer.
    @Test
    fun fake_dao_round_trips_null_totalNights_as_null() = runTest {
        val dao = FakeTourismCacheDao()
        val point = TourismDataPoint(
            countryCode = "PL",
            year = 2020,
            domesticNights = 1_000_000L,
            foreignNights = null,
            totalNights = null,
            trips = 50_000L,
        )
        dao.upsertAll(listOf(point), fetchedAt = 1_000_000L)

        val result = dao.query(TourismQuery(listOf("PL"), 2020..2020))
        assertEquals(1, result.size)
        assertNull(result[0].totalNights, "totalNights must survive round-trip as null, not be coerced to 0")
        assertEquals(50_000L, result[0].trips, "trips must be preserved alongside null totalNights")
    }

    // Verify that null foreignNights is also preserved (not just totalNights).
    @Test
    fun fake_dao_round_trips_null_foreignNights_as_null() = runTest {
        val dao = FakeTourismCacheDao()
        val point = TourismDataPoint(
            countryCode = "DE",
            year = 2021,
            domesticNights = 2_000_000L,
            foreignNights = null,
            totalNights = 3_500_000L,
            trips = null,
        )
        dao.upsertAll(listOf(point), fetchedAt = 2_000_000L)

        val result = dao.query(TourismQuery(listOf("DE"), 2021..2021))
        assertEquals(1, result.size)
        assertNull(result[0].foreignNights, "foreignNights must survive round-trip as null")
        assertEquals(2_000_000L, result[0].domesticNights)
    }

    // Verify that a point with ONLY trips (all nights null) is not silently lost.
    @Test
    fun fake_dao_preserves_trips_only_point_without_any_nights() = runTest {
        val dao = FakeTourismCacheDao()
        val point = TourismDataPoint(
            countryCode = "FR",
            year = 2022,
            domesticNights = null,
            foreignNights = null,
            totalNights = null,
            trips = 75_000L,
        )
        dao.upsertAll(listOf(point), fetchedAt = 3_000_000L)

        val result = dao.query(TourismQuery(listOf("FR"), 2022..2022))
        assertEquals(1, result.size, "Point with only trips and no nights must still be cached")
        assertNull(result[0].totalNights)
        assertNull(result[0].domesticNights)
        assertNull(result[0].foreignNights)
        assertEquals(75_000L, result[0].trips)
    }
}
