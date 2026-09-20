package eu.eurostat.ui.country

import eu.eurostat.core.common.EurostatCountries
import eu.eurostat.core.common.EurostatCountry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Locks down [compareLocalizedNames] and [sortedByDisplayName], the ordering behind the
 * country picker. Both are pure (no Compose host, no resources), so they run on the JVM and
 * on iosSimulatorArm64 alike; the Native run matters because the comparator relies on
 * `Char`/`String` case mapping.
 *
 * The name tables below mirror the `country_*` keys of the EN, PL and UK `strings.xml` verbatim.
 */
class CountryNameOrderTest {

    // Localized name per Eurostat code, in catalogue order (EU27_2020, EA20, then members).
    private val englishNames: Map<String, String> = mapOf(
        "EU27_2020" to "EU (27)",
        "EA20" to "Euro area (20)",
        "AT" to "Austria",
        "BE" to "Belgium",
        "BG" to "Bulgaria",
        "HR" to "Croatia",
        "CY" to "Cyprus",
        "CZ" to "Czechia",
        "DK" to "Denmark",
        "EE" to "Estonia",
        "FI" to "Finland",
        "FR" to "France",
        "DE" to "Germany",
        "EL" to "Greece",
        "HU" to "Hungary",
        "IE" to "Ireland",
        "IT" to "Italy",
        "LV" to "Latvia",
        "LT" to "Lithuania",
        "LU" to "Luxembourg",
        "MT" to "Malta",
        "NL" to "Netherlands",
        "PL" to "Poland",
        "PT" to "Portugal",
        "RO" to "Romania",
        "SK" to "Slovakia",
        "SI" to "Slovenia",
        "ES" to "Spain",
        "SE" to "Sweden",
        "IS" to "Iceland",
        "NO" to "Norway",
        "CH" to "Switzerland",
        "UK" to "United Kingdom",
        "TR" to "Türkiye",
    )
    private val englishSorted = listOf(
        "Austria",
        "Belgium",
        "Bulgaria",
        "Croatia",
        "Cyprus",
        "Czechia",
        "Denmark",
        "Estonia",
        "Finland",
        "France",
        "Germany",
        "Greece",
        "Hungary",
        "Iceland",
        "Ireland",
        "Italy",
        "Latvia",
        "Lithuania",
        "Luxembourg",
        "Malta",
        "Netherlands",
        "Norway",
        "Poland",
        "Portugal",
        "Romania",
        "Slovakia",
        "Slovenia",
        "Spain",
        "Sweden",
        "Switzerland",
        "Türkiye",
        "United Kingdom",
    )
    private val polishNames: Map<String, String> = mapOf(
        "EU27_2020" to "UE (27)",
        "EA20" to "Strefa euro (20)",
        "AT" to "Austria",
        "BE" to "Belgia",
        "BG" to "Bułgaria",
        "HR" to "Chorwacja",
        "CY" to "Cypr",
        "CZ" to "Czechy",
        "DK" to "Dania",
        "EE" to "Estonia",
        "FI" to "Finlandia",
        "FR" to "Francja",
        "DE" to "Niemcy",
        "EL" to "Grecja",
        "HU" to "Węgry",
        "IE" to "Irlandia",
        "IT" to "Włochy",
        "LV" to "Łotwa",
        "LT" to "Litwa",
        "LU" to "Luksemburg",
        "MT" to "Malta",
        "NL" to "Niderlandy",
        "PL" to "Polska",
        "PT" to "Portugalia",
        "RO" to "Rumunia",
        "SK" to "Słowacja",
        "SI" to "Słowenia",
        "ES" to "Hiszpania",
        "SE" to "Szwecja",
        "IS" to "Islandia",
        "NO" to "Norwegia",
        "CH" to "Szwajcaria",
        "UK" to "Wielka Brytania",
        "TR" to "Turcja",
    )
    private val polishSorted = listOf(
        "Austria",
        "Belgia",
        "Bułgaria",
        "Chorwacja",
        "Cypr",
        "Czechy",
        "Dania",
        "Estonia",
        "Finlandia",
        "Francja",
        "Grecja",
        "Hiszpania",
        "Irlandia",
        "Islandia",
        "Litwa",
        "Luksemburg",
        "Łotwa",
        "Malta",
        "Niderlandy",
        "Niemcy",
        "Norwegia",
        "Polska",
        "Portugalia",
        "Rumunia",
        "Słowacja",
        "Słowenia",
        "Szwajcaria",
        "Szwecja",
        "Turcja",
        "Węgry",
        "Wielka Brytania",
        "Włochy",
    )
    private val ukrainianNames: Map<String, String> = mapOf(
        "EU27_2020" to "ЄС (27)",
        "EA20" to "Єврозона (20)",
        "AT" to "Австрія",
        "BE" to "Бельгія",
        "BG" to "Болгарія",
        "HR" to "Хорватія",
        "CY" to "Кіпр",
        "CZ" to "Чехія",
        "DK" to "Данія",
        "EE" to "Естонія",
        "FI" to "Фінляндія",
        "FR" to "Франція",
        "DE" to "Німеччина",
        "EL" to "Греція",
        "HU" to "Угорщина",
        "IE" to "Ірландія",
        "IT" to "Італія",
        "LV" to "Латвія",
        "LT" to "Литва",
        "LU" to "Люксембург",
        "MT" to "Мальта",
        "NL" to "Нідерланди",
        "PL" to "Польща",
        "PT" to "Португалія",
        "RO" to "Румунія",
        "SK" to "Словаччина",
        "SI" to "Словенія",
        "ES" to "Іспанія",
        "SE" to "Швеція",
        "IS" to "Ісландія",
        "NO" to "Норвегія",
        "CH" to "Швейцарія",
        "UK" to "Велика Британія",
        "TR" to "Туреччина",
    )
    private val ukrainianSorted = listOf(
        "Австрія",
        "Бельгія",
        "Болгарія",
        "Велика Британія",
        "Греція",
        "Данія",
        "Естонія",
        "Ірландія",
        "Ісландія",
        "Іспанія",
        "Італія",
        "Кіпр",
        "Латвія",
        "Литва",
        "Люксембург",
        "Мальта",
        "Нідерланди",
        "Німеччина",
        "Норвегія",
        "Польща",
        "Португалія",
        "Румунія",
        "Словаччина",
        "Словенія",
        "Туреччина",
        "Угорщина",
        "Фінляндія",
        "Франція",
        "Хорватія",
        "Чехія",
        "Швейцарія",
        "Швеція",
    )

    private val allNameTables = listOf(englishNames, polishNames, ukrainianNames)

    private fun country(code: String, name: String) = EurostatCountry(code, name, flag = "")

    /** Localized names of [names]' countries in the order [sortedByDisplayName] returns them. */
    private fun sortedNames(names: Map<String, String>): List<String> =
        EurostatCountries.ALL.sortedByDisplayName(names).map { names.getValue(it.code) }

    // region compareLocalizedNames - Polish

    @Test
    fun polish_c_words_follow_h_then_y_then_z() {
        assertTrue(compareLocalizedNames("Chorwacja", "Cypr") < 0)
        assertTrue(compareLocalizedNames("Cypr", "Czechy") < 0)
    }

    @Test
    fun polish_barred_l_sorts_after_l() {
        assertTrue(compareLocalizedNames("Litwa", "Łotwa") < 0)
        assertTrue(compareLocalizedNames("Łotwa", "Litwa") > 0)
        assertTrue(compareLocalizedNames("Luksemburg", "Łotwa") < 0)
        assertTrue(compareLocalizedNames("Łotwa", "Malta") < 0)
    }

    @Test
    fun polish_w_words_order_e_then_i_then_barred_l() {
        assertTrue(compareLocalizedNames("Węgry", "Wielka Brytania") < 0)
        assertTrue(compareLocalizedNames("Wielka Brytania", "Włochy") < 0)
        assertTrue(compareLocalizedNames("Węgry", "Włochy") < 0)
    }

    @Test
    fun polish_barred_l_inside_a_word_sorts_after_l() {
        assertTrue(compareLocalizedNames("Bułgaria", "Chorwacja") < 0)
        assertTrue(compareLocalizedNames("Bulgaria", "Bułgaria") < 0)
        assertTrue(compareLocalizedNames("Bułgaria", "Buma") < 0)
        assertTrue(compareLocalizedNames("Słowacja", "Szwajcaria") < 0)
        assertTrue(compareLocalizedNames("Słowenia", "Szwecja") < 0)
        assertTrue(compareLocalizedNames("Słowacja", "Słowenia") < 0)
    }

    @Test
    fun polish_diacritic_letters_follow_their_base_letter() {
        assertTrue(compareLocalizedNames("Za", "Ża") < 0)
        assertTrue(compareLocalizedNames("Zz", "Źa") < 0)
        assertTrue(compareLocalizedNames("Źz", "Ża") < 0)
        assertTrue(compareLocalizedNames("Ca", "Ća") < 0)
        assertTrue(compareLocalizedNames("Cz", "Ća") < 0)
        assertTrue(compareLocalizedNames("Ćz", "Da") < 0)
        assertTrue(compareLocalizedNames("Az", "Ąa") < 0)
        assertTrue(compareLocalizedNames("Ąz", "Ba") < 0)
        assertTrue(compareLocalizedNames("Ez", "Ęa") < 0)
        assertTrue(compareLocalizedNames("Ęz", "Fa") < 0)
        assertTrue(compareLocalizedNames("Nz", "Ńa") < 0)
        assertTrue(compareLocalizedNames("Ńz", "Oa") < 0)
        assertTrue(compareLocalizedNames("Oz", "Óa") < 0)
        assertTrue(compareLocalizedNames("Óz", "Pa") < 0)
        assertTrue(compareLocalizedNames("Sz", "Śa") < 0)
        assertTrue(compareLocalizedNames("Śz", "Ta") < 0)
    }

    // endregion

    // region compareLocalizedNames - Ukrainian

    @Test
    fun ukrainian_i_sorts_after_y() {
        assertTrue(compareLocalizedNames("Ірландія", "Ісландія") < 0)
        assertTrue(compareLocalizedNames("Ісландія", "Іспанія") < 0)
        assertTrue(compareLocalizedNames("Іспанія", "Італія") < 0)
        assertTrue(compareLocalizedNames("Ив", "Іа") < 0)
    }

    @Test
    fun ukrainian_special_letters_follow_their_base_letter() {
        // ґ after г, before д
        assertTrue(compareLocalizedNames("Гя", "Ґа") < 0)
        assertTrue(compareLocalizedNames("Ґя", "Да") < 0)
        // є after е, before ж
        assertTrue(compareLocalizedNames("Ея", "Єа") < 0)
        assertTrue(compareLocalizedNames("Єя", "Жа") < 0)
        // ї after і, before й
        assertTrue(compareLocalizedNames("Ія", "Їа") < 0)
        assertTrue(compareLocalizedNames("Їя", "Йа") < 0)
        // і after и, before ї
        assertTrue(compareLocalizedNames("Ия", "Іа") < 0)
    }

    @Test
    fun ukrainian_soft_sign_and_late_letters() {
        assertTrue(compareLocalizedNames("Шя", "Ща") < 0)
        assertTrue(compareLocalizedNames("Щя", "Ьа") < 0)
        assertTrue(compareLocalizedNames("Ья", "Юа") < 0)
        assertTrue(compareLocalizedNames("Юя", "Яа") < 0)
    }

    // endregion

    // region compareLocalizedNames - English, case, separators

    @Test
    fun english_q_v_and_x_have_a_place() {
        // Luxembourg needs x, Slovakia and Slovenia need v: they must not tie with other letters.
        assertTrue(compareLocalizedNames("Luxembourg", "Malta") < 0)
        assertTrue(compareLocalizedNames("Luxembourg", "Lithuania") > 0)
        assertTrue(compareLocalizedNames("Slovakia", "Slovenia") < 0)
        assertTrue(compareLocalizedNames("Slovenia", "Spain") < 0)
        // Each of q, v and x must sit between its neighbours instead of ranking as an unknown letter.
        assertTrue(compareLocalizedNames("Pz", "Qa") < 0)
        assertTrue(compareLocalizedNames("Qz", "Ra") < 0)
        assertTrue(compareLocalizedNames("Uz", "Va") < 0)
        assertTrue(compareLocalizedNames("Vz", "Wa") < 0)
        assertTrue(compareLocalizedNames("Wz", "Xa") < 0)
        assertTrue(compareLocalizedNames("Xz", "Ya") < 0)
        assertTrue(compareLocalizedNames("Luxembourg", "Luzern") < 0)
    }

    @Test
    fun umlaut_u_sorts_as_plain_u() {
        assertEquals(0, compareLocalizedNames("Türkiye", "Turkiye"))
        assertTrue(compareLocalizedNames("Switzerland", "Türkiye") < 0)
        assertTrue(compareLocalizedNames("Türkiye", "United Kingdom") < 0)
        // ü is only folded to u, not treated as an extra letter after z.
        assertTrue(compareLocalizedNames("Tüa", "Tza") < 0)
    }

    @Test
    fun comparison_ignores_case_including_non_ascii_letters() {
        assertEquals(0, compareLocalizedNames("ł", "Ł"))
        assertEquals(0, compareLocalizedNames("abc", "ABC"))
        assertEquals(0, compareLocalizedNames("Łotwa", "łotwa"))
        assertEquals(0, compareLocalizedNames("ІРЛАНДІЯ", "ірландія"))
        assertEquals(0, compareLocalizedNames("Ґ", "ґ"))
    }

    @Test
    fun equal_strings_compare_equal_and_are_antisymmetric() {
        assertEquals(0, compareLocalizedNames("", ""))
        assertEquals(0, compareLocalizedNames("Polska", "Polska"))
        val pairs = listOf("Cypr" to "Czechy", "Łotwa" to "Litwa", "Ірландія" to "Іспанія", "UE (27)" to "UE")
        pairs.forEach { (a, b) ->
            val forward = compareLocalizedNames(a, b)
            val backward = compareLocalizedNames(b, a)
            assertTrue(forward != 0, "'$a' vs '$b' must not tie")
            assertEquals(forward < 0, backward > 0, "'$a' vs '$b' must be antisymmetric")
        }
    }

    @Test
    fun shorter_string_sorts_first_when_it_is_a_prefix() {
        assertTrue(compareLocalizedNames("Cypr", "Cypryjski") < 0)
        assertTrue(compareLocalizedNames("Cypryjski", "Cypr") > 0)
        assertTrue(compareLocalizedNames("UE", "UE (27)") < 0)
        assertTrue(compareLocalizedNames("UE (27)", "UE") > 0)
        assertTrue(compareLocalizedNames("", "A") < 0)
    }

    @Test
    fun separators_sort_before_letters_and_digits_sort_between() {
        assertTrue(compareLocalizedNames("Sri Lanka", "Srilanka") < 0)
        assertTrue(compareLocalizedNames("(a)", "a") < 0)
        assertTrue(compareLocalizedNames("A 1", "A1") < 0)
        assertTrue(compareLocalizedNames("A1", "Aa") < 0)
        assertTrue(compareLocalizedNames("A9", "Aa") < 0)
        assertTrue(compareLocalizedNames("A1", "A2") < 0)
    }

    @Test
    fun scripts_rank_latin_before_cyrillic_before_other_letters() {
        assertTrue(compareLocalizedNames("Zz", "Аа") < 0)
        assertTrue(compareLocalizedNames("Яя", "Ωω") < 0)
    }

    // endregion

    // region sortedByDisplayName - real catalogue

    @Test
    fun polish_country_names_are_alphabetical_after_the_aggregates() {
        assertEquals(polishSorted, sortedNames(polishNames).drop(2))
    }

    @Test
    fun polish_list_starts_with_the_documented_sequence() {
        assertEquals(
            listOf("Austria", "Belgia", "Bułgaria", "Chorwacja", "Cypr", "Czechy"),
            sortedNames(polishNames).drop(2).take(6),
        )
        assertEquals(
            listOf("Litwa", "Luksemburg", "Łotwa", "Malta"),
            sortedNames(polishNames).drop(2).dropWhile { it != "Litwa" }.take(4),
        )
        assertEquals(
            listOf("Turcja", "Węgry", "Wielka Brytania", "Włochy"),
            sortedNames(polishNames).takeLast(4),
        )
    }

    @Test
    fun ukrainian_country_names_are_alphabetical_after_the_aggregates() {
        assertEquals(ukrainianSorted, sortedNames(ukrainianNames).drop(2))
    }

    @Test
    fun ukrainian_i_names_are_contiguous_and_ordered() {
        assertEquals(
            listOf("Ірландія", "Ісландія", "Іспанія", "Італія"),
            sortedNames(ukrainianNames).filter { it.startsWith("І") },
        )
    }

    @Test
    fun english_country_names_are_alphabetical_after_the_aggregates() {
        assertEquals(englishSorted, sortedNames(englishNames).drop(2))
    }

    @Test
    fun english_efta_uk_and_turkiye_move_into_the_alphabet() {
        val names = sortedNames(englishNames)
        assertTrue(names.indexOf("Hungary") < names.indexOf("Iceland"))
        assertTrue(names.indexOf("Iceland") < names.indexOf("Ireland"))
        assertTrue(names.indexOf("Switzerland") < names.indexOf("Türkiye"))
        assertTrue(names.indexOf("Sweden") < names.indexOf("Switzerland"))
        assertTrue(names.indexOf("Türkiye") < names.indexOf("United Kingdom"))
        assertEquals("United Kingdom", names.last())
    }

    @Test
    fun aggregates_stay_pinned_first_in_catalogue_order_in_every_language() {
        allNameTables.forEach { names ->
            val ordered = EurostatCountries.ALL.sortedByDisplayName(names)
            assertEquals(listOf("EU27_2020", "EA20"), ordered.take(2).map { it.code }, names["EU27_2020"])
        }
    }

    @Test
    fun ukrainian_e_aggregate_is_pinned_rather_than_sorted_into_the_alphabet() {
        // "Єврозона (20)" and "ЄС (27)" start with Є, which would land mid-list if sorted.
        val ordered = EurostatCountries.ALL.sortedByDisplayName(ukrainianNames)
        assertEquals("ЄС (27)", ukrainianNames.getValue(ordered[0].code))
        assertEquals("Єврозона (20)", ukrainianNames.getValue(ordered[1].code))
        assertTrue(compareLocalizedNames("Єврозона (20)", "Естонія") > 0)
        val unpinned = EurostatCountries.ALL.sortedByDisplayName(ukrainianNames, pinnedFirst = emptySet())
        val euroAreaIndex = unpinned.indexOfFirst { it.code == "EA20" }
        assertTrue(euroAreaIndex > 1, "unpinned, the euro area sorts into the alphabet (index $euroAreaIndex)")
    }

    @Test
    fun result_does_not_depend_on_input_order_beyond_the_pinned_codes() {
        allNameTables.forEach { names ->
            val forward = EurostatCountries.ALL.sortedByDisplayName(names).drop(2)
            val backward = EurostatCountries.ALL.reversed().sortedByDisplayName(names).drop(2)
            assertEquals(forward, backward)
        }
    }

    @Test
    fun catalogue_order_is_not_alphabetical_in_polish_which_is_the_bug_being_fixed() {
        val cataloguePl = EurostatCountries.ALL.map { polishNames.getValue(it.code) }
        assertNotEquals(cataloguePl, sortedNames(polishNames))
        // Concretely: the catalogue puts Niemcy (Germany) before Grecja (Greece).
        assertTrue(cataloguePl.indexOf("Niemcy") < cataloguePl.indexOf("Grecja"))
        val sortedPl = sortedNames(polishNames)
        assertTrue(sortedPl.indexOf("Grecja") < sortedPl.indexOf("Niemcy"))
    }

    // endregion

    // region sortedByDisplayName - stability and totality

    @Test
    fun result_keeps_every_country_exactly_once() {
        allNameTables.forEach { names ->
            val ordered = EurostatCountries.ALL.sortedByDisplayName(names)
            assertEquals(EurostatCountries.ALL.size, ordered.size)
            assertEquals(EurostatCountries.ALL.map { it.code }.toSet(), ordered.map { it.code }.toSet())
        }
    }

    @Test
    fun empty_display_names_fall_back_to_the_english_catalogue_name() {
        val ordered = EurostatCountries.ALL.sortedByDisplayName(emptyMap())
        assertEquals(listOf("EU27_2020", "EA20"), ordered.take(2).map { it.code })
        assertEquals(englishSorted, ordered.drop(2).map { it.name })
    }

    @Test
    fun a_code_missing_from_display_names_sorts_by_its_english_name() {
        // Only Poland is translated (to something that sorts last); everything else falls back.
        val ordered = EurostatCountries.ALL.sortedByDisplayName(mapOf("PL" to "Zzz"))
        assertEquals("PL", ordered.last().code)
        val rest = ordered.drop(2).dropLast(1).map { it.name }
        assertEquals(englishSorted.filter { it != "Poland" }, rest)
    }

    @Test
    fun equal_names_keep_their_input_order() {
        val names = mapOf("XA" to "Same", "XB" to "Same", "XC" to "Alpha", "XD" to "same")
        val input = listOf(country("XA", "a"), country("XB", "b"), country("XC", "c"), country("XD", "d"))
        assertEquals(listOf("XC", "XA", "XB", "XD"), input.sortedByDisplayName(names).map { it.code })
        assertEquals(
            listOf("XC", "XD", "XB", "XA"),
            input.reversed().sortedByDisplayName(names).map { it.code },
        )
    }

    @Test
    fun pinned_codes_are_configurable_and_keep_input_order() {
        val names = mapOf("XA" to "Zulu", "XB" to "Alpha", "XC" to "Mike")
        val input = listOf(country("XA", "a"), country("XB", "b"), country("XC", "c"))
        // Pinned codes keep input order (XA before XC), then the rest by name.
        assertEquals(
            listOf("XA", "XC", "XB"),
            input.sortedByDisplayName(names, pinnedFirst = setOf("XC", "XA")).map { it.code },
        )
        assertEquals(
            listOf("XB", "XC", "XA"),
            input.sortedByDisplayName(names, pinnedFirst = emptySet()).map { it.code },
        )
    }

    @Test
    fun empty_list_sorts_to_an_empty_list() {
        assertEquals(emptyList<EurostatCountry>(), emptyList<EurostatCountry>().sortedByDisplayName(polishNames))
    }

    // endregion
}
