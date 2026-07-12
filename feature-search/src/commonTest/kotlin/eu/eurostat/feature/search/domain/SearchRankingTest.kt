package eu.eurostat.feature.search.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for the pure ranking pipeline: tier ordering (label prefix beats
 * word-prefix beats substring beats keyword beats description), case
 * handling, blank-query behavior and the browse grouping — plus sanity
 * checks over the real [SearchIndex].
 */
class SearchRankingTest {

    // -----------------------------------------------------------------------
    // Synthetic entries crafted so the query "gas" hits exactly one tier each.
    // -----------------------------------------------------------------------

    private fun entry(
        id: String,
        label: String,
        description: String = "irrelevant text",
        keywords: List<String> = emptyList(),
        module: SearchModule = SearchModule.ECONOMY,
    ) = IndicatorEntry(
        id = id,
        label = label,
        module = module,
        datasetCode = "code_$id",
        description = description,
        keywords = keywords,
    )

    private val labelPrefix = entry("prefix", "Gas prices")
    private val wordPrefix = entry("word", "Natural gas imports")
    private val substring = entry("substring", "Megastore turnover")
    private val keywordMatch = entry("keyword", "Household energy", keywords = listOf("gas", "heating"))
    private val descriptionMatch = entry("description", "Pipelines", description = "Cross-border gas transit")
    private val noMatch = entry("none", "Olive harvest")

    /** Deliberately listed worst-tier first to prove ordering is by tier, not index. */
    private val synthetic = listOf(noMatch, descriptionMatch, keywordMatch, substring, wordPrefix, labelPrefix)

    @Test
    fun ranking_orders_tiers_prefix_word_substring_keyword_description() {
        val results = rankIndicators("gas", synthetic)
        assertEquals(
            listOf(labelPrefix, wordPrefix, substring, keywordMatch, descriptionMatch),
            results,
        )
    }

    @Test
    fun non_matching_entries_are_excluded() {
        val results = rankIndicators("gas", synthetic)
        assertTrue(noMatch !in results, "unrelated entry must not appear")
    }

    @Test
    fun matching_is_case_insensitive_and_trims_whitespace() {
        assertEquals(
            rankIndicators("gas", synthetic),
            rankIndicators("  GaS  ", synthetic),
        )
    }

    @Test
    fun blank_query_returns_no_ranked_results() {
        assertEquals(emptyList(), rankIndicators("", synthetic))
        assertEquals(emptyList(), rankIndicators("   ", synthetic))
    }

    @Test
    fun unmatched_query_returns_empty_list() {
        assertEquals(emptyList(), rankIndicators("zzzz", synthetic))
    }

    @Test
    fun ties_within_a_tier_preserve_index_order() {
        val first = entry("first", "Gas grid density")
        val second = entry("second", "Gas storage levels")
        val results = rankIndicators("gas", listOf(first, second))
        assertEquals(listOf(first, second), results)
    }

    // -----------------------------------------------------------------------
    // Browse grouping
    // -----------------------------------------------------------------------

    @Test
    fun browse_sections_group_by_module_in_declaration_order() {
        val economy = entry("e1", "GDP", module = SearchModule.ECONOMY)
        val social = entry("s1", "Poverty", module = SearchModule.SOCIAL)
        val population = entry("p1", "Population", module = SearchModule.POPULATION)

        val sections = browseSections(listOf(economy, social, population))

        assertEquals(
            listOf(SearchModule.POPULATION, SearchModule.ECONOMY, SearchModule.SOCIAL),
            sections.map { it.module },
        )
        assertEquals(listOf(population), sections[0].entries)
        assertEquals(listOf(economy), sections[1].entries)
    }

    @Test
    fun browse_sections_omit_modules_without_entries() {
        val sections = browseSections(listOf(entry("only", "GDP", module = SearchModule.ECONOMY)))
        assertEquals(listOf(SearchModule.ECONOMY), sections.map { it.module })
    }

    // -----------------------------------------------------------------------
    // Real index sanity
    // -----------------------------------------------------------------------

    @Test
    fun index_covers_all_eight_modules_with_unique_ids() {
        assertEquals(
            SearchModule.entries.toSet(),
            SearchIndex.entries.map { it.module }.toSet(),
            "every module must ship at least one indicator",
        )
        assertEquals(
            SearchIndex.entries.size,
            SearchIndex.entries.map { it.id }.toSet().size,
            "indicator ids must be unique",
        )
        assertTrue(SearchIndex.entries.size >= 25, "index should cover ~25+ indicators")
    }

    @Test
    fun natural_language_queries_hit_the_expected_indicators() {
        val inflation = rankIndicators("inflation")
        assertEquals("hicp-inflation", inflation.first().id)

        val co2 = rankIndicators("co2")
        assertTrue(co2.isNotEmpty() && co2.all { it.module == SearchModule.ENVIRONMENT })

        val hotels = rankIndicators("hotels")
        assertTrue(hotels.any { it.module == SearchModule.TOURISM })
    }

    @Test
    fun label_word_prefix_beats_description_hit_on_the_real_index() {
        // "gdp" is a word inside "Gross domestic product (GDP)" but only a
        // description hit on R&D expenditure ("% of GDP") — GDP ranks first.
        val results = rankIndicators("gdp")
        assertEquals("gdp", results.first().id)
        assertTrue(results.any { it.id == "rd-intensity" }, "description tier should still surface R&D")
    }
}
