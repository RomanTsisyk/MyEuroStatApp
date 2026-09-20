package eu.eurostat.ui.country

import eu.eurostat.core.common.EurostatCountry

/**
 * Latin alphabet in Polish letter order (diacritic letters are separate letters placed
 * right after their base letter). Also carries `q`, `v` and `x`, which Polish lacks but
 * English names such as Luxembourg or Slovakia need.
 */
private const val LATIN_ALPHABET = "aąbcćdeęfghijklłmnńoópqrsśtuvwxyzźż"

/** Ukrainian alphabet in dictionary order: `ґ` after `г`, `є` after `е`, `і` after `и`, `ї` after `і`. */
private const val CYRILLIC_ALPHABET = "абвгґдеєжзиіїйклмнопрстуфхцчшщьюя"

// Rank bands, lowest first. Every band is 0x10000 wide so a Char code point (at most
// 0xFFFF) or an alphabet index never spills into the next band.
private const val BAND_WIDTH = 0x10000
private const val RANK_DIGIT = 1 * BAND_WIDTH
private const val RANK_LATIN = 2 * BAND_WIDTH
private const val RANK_CYRILLIC = 3 * BAND_WIDTH
private const val RANK_OTHER_LETTER = 4 * BAND_WIDTH

/**
 * Sort rank of one already-lowercased character.
 *
 * Bands, lowest first: non-letters (space, parentheses, ...) by code point, digits by code
 * point, Latin/Polish letters by [LATIN_ALPHABET] position, Cyrillic/Ukrainian letters by
 * [CYRILLIC_ALPHABET] position, then any other letter by code point.
 */
private fun charRank(c: Char): Int {
    // The only accent that has to be folded for the current catalogue: "Türkiye" sorts as "Turkiye".
    val folded = if (c == 'ü') 'u' else c
    val latin = LATIN_ALPHABET.indexOf(folded)
    if (latin >= 0) return RANK_LATIN + latin
    val cyrillic = CYRILLIC_ALPHABET.indexOf(folded)
    if (cyrillic >= 0) return RANK_CYRILLIC + cyrillic
    return when {
        folded.isDigit() -> RANK_DIGIT + folded.code
        folded.isLetter() -> RANK_OTHER_LETTER + folded.code
        else -> folded.code
    }
}

/**
 * Compares two already-localized display names in dictionary order, case-insensitively.
 *
 * Kotlin common has no `Collator`, and core-ui cannot see the active app locale, so the
 * comparison works on one combined alphabet table instead. Polish and English names are
 * Latin and Ukrainian names are Cyrillic, so the script of the text alone selects the
 * right letter order:
 * - Polish diacritic letters are separate letters: `Łotwa` sorts after `Luksemburg`,
 *   `Węgry` before `Wielka Brytania` before `Włochy`.
 * - Ukrainian `ґ`, `є`, `і`, `ї` sit after `г`, `е`, `и`, `і`: `Ірландія` < `Ісландія` <
 *   `Іспанія` < `Італія`.
 * - `ü` is folded to `u`, so `Türkiye` sorts under `T`.
 *
 * Characters are ranked in this order: non-letters (space, parentheses) by code point,
 * digits, Latin letters, Cyrillic letters, any other letter by code point. When one name
 * is a prefix of the other, the shorter one sorts first.
 *
 * @return a negative number when [a] sorts before [b], zero when they compare equal
 *   (differing only in case, or in `ü` versus `u`), a positive number when [a] sorts after [b].
 */
fun compareLocalizedNames(a: String, b: String): Int {
    val left = a.lowercase()
    val right = b.lowercase()
    val shared = minOf(left.length, right.length)
    for (i in 0 until shared) {
        val diff = charRank(left[i]).compareTo(charRank(right[i]))
        if (diff != 0) return diff
    }
    return left.length.compareTo(right.length)
}

/**
 * Orders countries for display: the [pinnedFirst] aggregates first, in their existing
 * (catalogue) order, then every other country by its localized name under
 * [compareLocalizedNames].
 *
 * The sort is stable, so countries with equal names keep their catalogue order.
 *
 * @param displayNames localized display name per Eurostat code (see [countryDisplayName]).
 *   A code missing from the map falls back to [EurostatCountry.name], so a partial map
 *   never throws.
 * @param pinnedFirst codes that stay at the top of the list, unsorted. Defaults to the two
 *   aggregates, `EU27_2020` and `EA20`.
 */
fun List<EurostatCountry>.sortedByDisplayName(
    displayNames: Map<String, String>,
    pinnedFirst: Set<String> = setOf("EU27_2020", "EA20"),
): List<EurostatCountry> {
    val (pinned, others) = partition { it.code in pinnedFirst }
    return pinned + others.sortedWith { a, b ->
        compareLocalizedNames(displayNames[a.code] ?: a.name, displayNames[b.code] ?: b.name)
    }
}
