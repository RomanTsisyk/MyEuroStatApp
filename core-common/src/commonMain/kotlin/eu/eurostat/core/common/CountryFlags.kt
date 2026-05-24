package eu.eurostat.core.common

/**
 * Converts a Eurostat country code to its Unicode flag emoji.
 *
 * Handles Eurostat-specific codes that differ from ISO 3166-1 alpha-2:
 * - `EU27_2020` → 🇪🇺 (European Union flag)
 * - `EA20`      → "EA" text (no emoji equivalent)
 * - `EL`        → 🇬🇷 (Greece uses EL in Eurostat, GR in ISO)
 * - `UK`        → 🇬🇧 (United Kingdom uses UK in Eurostat, GB in ISO)
 *
 * For all other standard ISO 3166-1 alpha-2 codes the regional indicator
 * symbols are computed arithmetically from the character code points.
 *
 * @param eurostatCode the Eurostat country/area code (e.g. `"DE"`, `"EU27_2020"`).
 * @return a Unicode flag emoji string, or the original code as fallback.
 */
fun flagFor(eurostatCode: String): String = when (eurostatCode.uppercase()) {
    "EU27_2020", "EU28", "EU27", "EU" -> euFlag()
    "EA20", "EA19", "EA18", "EA" -> "EA"
    "EL" -> isoToFlag("GR")
    "UK" -> isoToFlag("GB")
    else -> if (eurostatCode.length == 2) isoToFlag(eurostatCode) else eurostatCode
}

/**
 * Converts a standard ISO 3166-1 alpha-2 code to a Unicode flag emoji.
 *
 * Each letter is mapped to its corresponding Regional Indicator Symbol Letter
 * (U+1F1E6..U+1F1FF). The two code points together form a flag sequence
 * encoded as surrogate pairs so the result is a valid Kotlin/JVM/JS/Native
 * `String` on all KMP targets.
 *
 * @param iso2 two-letter ISO country code (e.g. `"DE"`).
 * @return flag emoji string, or [iso2] unchanged if it is not exactly 2 ASCII letters.
 */
fun isoToFlag(iso2: String): String {
    if (iso2.length != 2) return iso2
    val a = iso2[0].uppercaseChar()
    val b = iso2[1].uppercaseChar()
    if (a !in 'A'..'Z' || b !in 'A'..'Z') return iso2
    val first = 0x1F1E6 + (a.code - 'A'.code)
    val second = 0x1F1E6 + (b.code - 'A'.code)
    return codePointsToString(first, second)
}

/** Returns the EU flag emoji 🇪🇺. */
private fun euFlag(): String = isoToFlag("EU")

/**
 * Converts two Unicode code points to a [String] using UTF-16 surrogate pairs.
 * Avoids `String(intArrayOf, offset, count)` which is not available on all KMP targets.
 */
private fun codePointsToString(vararg codePoints: Int): String = buildString {
    for (cp in codePoints) {
        if (cp <= 0xFFFF) {
            append(cp.toChar())
        } else {
            // Supplementary plane: encode as surrogate pair
            val offset = cp - 0x10000
            append(((offset ushr 10) + 0xD800).toChar())
            append(((offset and 0x3FF) + 0xDC00).toChar())
        }
    }
}
