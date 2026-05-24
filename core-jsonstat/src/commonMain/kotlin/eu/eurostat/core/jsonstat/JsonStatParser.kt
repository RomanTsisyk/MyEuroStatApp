package eu.eurostat.core.jsonstat

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull

/**
 * One flattened cell from a JSON-stat response.
 *
 * Generic by design — no feature-specific fields. Feature modules adapt this into their
 * own domain types (e.g. PopulationDataPoint).
 *
 * Example: for a population dataset with dimensions [geo, time, sex], a single cell might be
 *   dimensions      = {"geo" -> "PL", "time" -> "2023", "sex" -> "F"}
 *   dimensionLabels = {"geo" -> "Poland", "time" -> "2023", "sex" -> "Female"}
 *   value           = 19_345_000.0
 *
 * [dimensionLabels] holds the human-readable label for each dimension value, sourced from the
 * JSON-stat `category.label` map. Falls back to the code itself when no label is provided.
 * Defaults to [emptyMap] for backward-compatibility with code written before this field existed.
 */
data class JsonStatCell(
    val dimensions: Map<String, String>,
    val dimensionLabels: Map<String, String> = emptyMap(),
    val value: Double?,
)

/**
 * Generic JSON-stat 2.0 unpacker. The hot path: convert a flat value map into typed cells.
 *
 * Algorithm (see jsonstat_parsing_flow diagram):
 *   1. From `size`, compute a multiplier per dimension: mult[d] = product(size[d+1..n-1]).
 *      For size [27, 24, 3] this gives [72, 3, 1].
 *   2. For each value entry with key i:
 *        index[d] = (i / mult[d]) % size[d]
 *      Resolve each index back to its category label using the reversed `dimension[d].category.index` map.
 *   3. Emit a JsonStatCell carrying the label map and the numeric value.
 *
 * Complexity: O(n × d) where n = number of values, d = number of dimensions.
 * For Eurostat datasets (n typically 10^3 – 10^5, d typically 2 – 4) this is sub-millisecond on device.
 *
 * Null values: JSON-stat encodes "no observation" as JSON null. We preserve those as `value = null`
 * so callers can decide whether to skip or render a gap.
 */
class JsonStatParser {

    fun parse(response: JsonStatResponse): List<JsonStatCell> {
        val sizes = response.size
        require(sizes.isNotEmpty()) { "JSON-stat response must declare at least one dimension" }
        require(sizes.size == response.id.size) {
            "id.size (${response.id.size}) must match size.size (${sizes.size})"
        }

        val multipliers = computeMultipliers(sizes)
        val labelLookups = buildLabelLookups(response)
        val categoryLabels = buildCategoryLabelMaps(response)

        return when (val v = response.value) {
            is JsonObject -> v.entries.mapNotNull { (key, element) ->
                val flatIndex = key.toIntOrNull() ?: return@mapNotNull null
                buildCell(flatIndex, multipliers, response.id, labelLookups, categoryLabels, element.asDoubleOrNull())
            }
            is JsonArray -> v.mapIndexed { flatIndex, element ->
                buildCell(flatIndex, multipliers, response.id, labelLookups, categoryLabels, element.asDoubleOrNull())
            }
            else -> error("JSON-stat `value` must be object or array, got ${v::class.simpleName}")
        }
    }

    /**
     * For size [a, b, c], returns [b*c, c, 1]. The last multiplier is always 1.
     * Walks right-to-left accumulating the product.
     */
    internal fun computeMultipliers(sizes: List<Int>): IntArray {
        val out = IntArray(sizes.size)
        var acc = 1L
        for (d in sizes.indices.reversed()) {
            require(acc <= Int.MAX_VALUE) { "JSON-stat dimension product overflows Int (sizes=$sizes)" }
            out[d] = acc.toInt()
            acc *= sizes[d]
        }
        return out
    }

    /**
     * For each dimension, build a reverse map: position-index -> category key (code).
     * Source map is category-key -> position-index, so we invert it.
     */
    private fun buildLabelLookups(response: JsonStatResponse): Map<String, Array<String>> {
        return response.id.associateWith { dimId ->
            val cat = response.dimension.getValue(dimId).category
            val reversed = arrayOfNulls<String>(cat.index.size)
            for ((key, pos) in cat.index) reversed[pos] = key
            @Suppress("UNCHECKED_CAST")
            reversed as Array<String>
        }
    }

    /**
     * For each dimension, build a map: category code -> human-readable label.
     * When the JSON-stat response has no `label` map for a dimension, returns an empty map
     * for that dimension so callers can safely fall back to the code.
     */
    private fun buildCategoryLabelMaps(response: JsonStatResponse): Map<String, Map<String, String>> {
        return response.id.associateWith { dimId ->
            response.dimension.getValue(dimId).category.label ?: emptyMap()
        }
    }

    private fun buildCell(
        flatIndex: Int,
        multipliers: IntArray,
        dimensionIds: List<String>,
        lookups: Map<String, Array<String>>,
        categoryLabels: Map<String, Map<String, String>>,
        value: Double?,
    ): JsonStatCell {
        val dims = HashMap<String, String>(dimensionIds.size)
        val labels = HashMap<String, String>(dimensionIds.size)
        for (d in dimensionIds.indices) {
            val dimId = dimensionIds[d]
            val pos = (flatIndex / multipliers[d]) % lookups.getValue(dimId).size
            val code = lookups.getValue(dimId)[pos]
            dims[dimId] = code
            // Resolve human-readable label; fall back to the code if absent
            labels[dimId] = categoryLabels[dimId]?.get(code) ?: code
        }
        return JsonStatCell(dims, labels, value)
    }

    private fun kotlinx.serialization.json.JsonElement.asDoubleOrNull(): Double? = when (this) {
        is JsonNull -> null
        is JsonPrimitive -> doubleOrNull
        else -> null
    }
}
