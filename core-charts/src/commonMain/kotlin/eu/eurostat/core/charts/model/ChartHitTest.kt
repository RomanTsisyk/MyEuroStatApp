package eu.eurostat.core.charts.model

import androidx.compose.ui.geometry.Offset

/**
 * Pure hit-test for line-based charts: finds the rendered [ChartPoint] closest to
 * a tap, if any lies within [thresholdPx].
 *
 * The function is deliberately UI-free and unit-testable — it never touches the
 * Canvas. Callers supply the very same value → pixel projection the draw pass
 * uses via [mapX] / [mapY] (so the hit-test and the drawn line can never drift),
 * plus the tap position and a pixel radius.
 *
 * Semantics:
 * - Points with a `null` [ChartPoint.y] (gaps) are skipped — you cannot tap a gap.
 * - Distance is Euclidean in pixel space; the nearest qualifying point wins.
 * - A point qualifies only when its distance is `<= thresholdPx`; taps farther
 *   than the threshold from every point return `null`.
 * - On an exact distance tie the first-encountered point (earlier series, then
 *   earlier point) wins, so the result is deterministic.
 * - Empty [series], all-empty series, or an all-`null` series return `null`.
 *
 * @param series The rendered series, in draw order.
 * @param tap The tap position in the chart's local pixel coordinates (same origin
 *            as [mapX]/[mapY] output — i.e. the Canvas content box).
 * @param mapX Projects a point's [ChartPoint.x] to its pixel X, identical to the draw pass.
 * @param mapY Projects a point's (non-null) [ChartPoint.y] to its pixel Y, identical to the draw pass.
 * @param thresholdPx Maximum pixel distance for a tap to register as a hit.
 * @return The matched `(series, point)` pair, or `null` when nothing is within range.
 */
fun nearestChartPoint(
    series: List<ChartSeries>,
    tap: Offset,
    mapX: (Double) -> Float,
    mapY: (Double) -> Float,
    thresholdPx: Float,
): Pair<ChartSeries, ChartPoint>? {
    // Compare squared distances to avoid a sqrt per candidate (Native-friendly,
    // and monotonic so it preserves nearest ordering).
    val thresholdSq = thresholdPx * thresholdPx
    var best: Pair<ChartSeries, ChartPoint>? = null
    var bestDistSq = Float.MAX_VALUE
    series.forEach { s ->
        s.points.forEach { p ->
            val yv = p.y ?: return@forEach // skip gaps — nothing drawn to tap
            val dx = mapX(p.x) - tap.x
            val dy = mapY(yv) - tap.y
            val distSq = dx * dx + dy * dy
            // Strict `<` keeps the first-encountered point on ties (deterministic).
            if (distSq <= thresholdSq && distSq < bestDistSq) {
                bestDistSq = distSq
                best = s to p
            }
        }
    }
    return best
}
