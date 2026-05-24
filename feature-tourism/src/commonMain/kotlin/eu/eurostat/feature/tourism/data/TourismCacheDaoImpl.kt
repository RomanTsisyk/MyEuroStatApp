package eu.eurostat.feature.tourism.data

import eu.eurostat.core.database.generated.AppDatabase
import eu.eurostat.feature.tourism.domain.TourismDataPoint
import eu.eurostat.feature.tourism.domain.TourismQuery
import eu.eurostat.feature.tourism.domain.TourismResidence

/**
 * SQLDelight-backed cache for tourism rows.
 *
 * The underlying table is residence-tall (one row per (country, year, residence))
 * because that's the shape `TourismCache.sq` was created with. The DAO exposes
 * the wide model used by the rest of the feature: it persists each
 * [TourismDataPoint] as up-to-three entity rows (one per non-null residence
 * column) and rehydrates by merging the three residences back into one wide
 * row per (country, year). The `trips` value, which is independent of
 * residence in the upstream Eurostat dataset, is stored on the `Total` row
 * only to avoid triple-counting.
 *
 * ## Null-nights sentinel
 * The schema column `nights` is `INTEGER NOT NULL` — it cannot store SQL NULL.
 * When a Total row exists only because `trips` is present (i.e. `totalNights`
 * is null), we write [NIGHTS_NULL_SENTINEL] (-1) as the stored value.
 * On read, [NIGHTS_NULL_SENTINEL] is converted back to `null` so that the
 * domain model correctly distinguishes "data unavailable" from "zero nights".
 * This avoids schema migrations while preserving data integrity.
 */
class TourismCacheDaoImpl(
    private val db: AppDatabase,
) : TourismCacheDao {

    override suspend fun query(query: TourismQuery): List<TourismDataPoint> {
        val queries = db.tourismCacheQueries
        val yearFrom = query.yearRange.first.toLong()
        val yearTo = query.yearRange.last.toLong()

        val wide = mutableMapOf<Pair<String, Int>, TourismDataPoint>()
        for (residence in TourismResidence.entries) {
            val entities = queries.queryByCountriesAndYears(
                country_code = query.countryCodes,
                year = yearFrom,
                year_ = yearTo,
                residence = residence.code,
            ).executeAsList()
            for (e in entities) {
                val key = e.country_code to e.year.toInt()
                val existing = wide[key] ?: TourismDataPoint(
                    countryCode = e.country_code,
                    year = e.year.toInt(),
                )
                // Convert the sentinel back to null: NIGHTS_NULL_SENTINEL means
                // "no nights data was available when this row was cached".
                val nightsOrNull = e.nights.takeIf { it != NIGHTS_NULL_SENTINEL }
                val withNights = when (residence) {
                    TourismResidence.Domestic -> existing.copy(domesticNights = nightsOrNull)
                    TourismResidence.Foreign -> existing.copy(foreignNights = nightsOrNull)
                    TourismResidence.Total -> existing.copy(totalNights = nightsOrNull)
                }
                // trips is residence-agnostic; we only persisted it on the Total row.
                wide[key] = if (residence == TourismResidence.Total && e.trips != null) {
                    withNights.copy(trips = e.trips)
                } else {
                    withNights
                }
            }
        }
        return wide.values.sortedWith(compareBy({ it.countryCode }, { it.year }))
    }

    override suspend fun upsertAll(rows: List<TourismDataPoint>, fetchedAt: Long) {
        val queries = db.tourismCacheQueries
        db.transaction {
            for (row in rows) {
                row.domesticNights?.let { nights ->
                    queries.upsert(
                        country_code = row.countryCode,
                        year = row.year.toLong(),
                        nights = nights,
                        trips = null,
                        residence = TourismResidence.Domestic.code,
                        fetched_at_epoch_ms = fetchedAt,
                    )
                }
                row.foreignNights?.let { nights ->
                    queries.upsert(
                        country_code = row.countryCode,
                        year = row.year.toLong(),
                        nights = nights,
                        trips = null,
                        residence = TourismResidence.Foreign.code,
                        fetched_at_epoch_ms = fetchedAt,
                    )
                }
                // Persist trips alongside the Total row so it is read back once,
                // not three times. When totalNights is null but trips is present
                // we write NIGHTS_NULL_SENTINEL (-1) instead of 0L so that the
                // read path can distinguish "no data" from "actually zero nights".
                val totalNights = row.totalNights
                val trips = row.trips
                if (totalNights != null || trips != null) {
                    queries.upsert(
                        country_code = row.countryCode,
                        year = row.year.toLong(),
                        nights = totalNights ?: NIGHTS_NULL_SENTINEL,
                        trips = trips,
                        residence = TourismResidence.Total.code,
                        fetched_at_epoch_ms = fetchedAt,
                    )
                }
            }
        }
    }

    companion object {
        /**
         * Sentinel stored in the `nights INTEGER NOT NULL` column when the source
         * domain object had `totalNights = null` but `trips != null`.
         *
         * `-1L` is used because it is outside the valid range of any Eurostat
         * nights count (which is always ≥ 0). On read, any stored value equal to
         * this sentinel is converted back to `null` in the domain model.
         *
         * Exposed as `internal` so that unit tests in the same module can assert
         * the sentinel value without coupling to the magic literal directly.
         */
        internal const val NIGHTS_NULL_SENTINEL = -1L

        /** Test-visible alias for [NIGHTS_NULL_SENTINEL]. */
        @Suppress("unused")
        val NIGHTS_NULL_SENTINEL_FOR_TEST: Long get() = NIGHTS_NULL_SENTINEL
    }

    override suspend fun oldestFetchedAt(query: TourismQuery): Long? {
        val queries = db.tourismCacheQueries
        val yearFrom = query.yearRange.first.toLong()
        val yearTo = query.yearRange.last.toLong()
        var min: Long? = null
        for (residence in TourismResidence.entries) {
            val candidate = queries.oldestFetchedAt(
                country_code = query.countryCodes,
                year = yearFrom,
                year_ = yearTo,
                residence = residence.code,
            ).executeAsOneOrNull()?.oldest
            if (candidate != null) {
                val cur = min
                min = if (cur == null) candidate else minOf(cur, candidate)
            }
        }
        return min
    }
}
