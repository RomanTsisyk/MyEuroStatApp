package eu.eurostat.core.jsonstat

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Raw JSON-stat 2.0 response shape. Mirrors the Eurostat REST API format.
 *
 * Reference: https://json-stat.org/format/
 *
 * Example minimal payload:
 * ```json
 * {
 *   "id":     ["geo", "time", "sex"],
 *   "size":   [27, 24, 3],
 *   "dimension": {
 *     "geo":  { "category": { "index": { "AT": 0, "BE": 1, ... }, "label": { "AT": "Austria", ... } } },
 *     "time": { "category": { "index": { "2000": 0, "2001": 1, ... } } },
 *     "sex":  { "category": { "index": { "T": 0, "M": 1, "F": 2 } } }
 *   },
 *   "value": { "0": 8000000, "1": 4000000, ... }
 * }
 * ```
 *
 * `value` may be either a JSON object (sparse, with string keys) or a JSON array (dense).
 * The parser handles both — that's why we deserialize it as raw [JsonElement] here.
 */
@Serializable
data class JsonStatResponse(
    val id: List<String>,
    val size: List<Int>,
    val dimension: Map<String, JsonStatDimension>,
    val value: JsonElement,
    val label: String? = null,
    val source: String? = null,
    val updated: String? = null,
)

@Serializable
data class JsonStatDimension(
    val label: String? = null,
    val category: JsonStatCategory,
)

@Serializable
data class JsonStatCategory(
    val index: Map<String, Int>,
    val label: Map<String, String>? = null,
)
