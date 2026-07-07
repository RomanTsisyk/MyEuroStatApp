package eu.eurostat.feature.search.domain

/**
 * Static index of every indicator the eight feature modules ship.
 *
 * Derived from the dataset table in `CLAUDE.md` (all codes verified against
 * the live Eurostat API there — keep the two in sync when a module gains or
 * drops an indicator). The index is compiled in rather than fetched: the set
 * of indicators only changes with an app release, so a Kotlin list keeps the
 * Search screen fully offline and instantly responsive.
 */
object SearchIndex {

    /** All searchable indicators, in canonical module order. */
    val entries: List<IndicatorEntry> = listOf(
        // ------------------------------------------------------------------
        // Population — demo_pjangroup
        // ------------------------------------------------------------------
        IndicatorEntry(
            id = "population-total",
            label = "Total population",
            module = SearchModule.POPULATION,
            datasetCode = "demo_pjangroup",
            description = "Population on 1 January, total across all ages",
            keywords = listOf("population", "people", "inhabitants", "residents", "demography", "citizens"),
        ),
        IndicatorEntry(
            id = "population-age-pyramid",
            label = "Population by age cohort",
            module = SearchModule.POPULATION,
            datasetCode = "demo_pjangroup",
            description = "18 five-year age cohorts driving the population pyramid",
            keywords = listOf("age", "cohort", "pyramid", "ageing", "aging", "elderly", "young", "generation"),
        ),
        IndicatorEntry(
            id = "population-by-sex",
            label = "Population by sex",
            module = SearchModule.POPULATION,
            datasetCode = "demo_pjangroup",
            description = "Male and female population split per age cohort",
            keywords = listOf("sex", "gender", "men", "women", "male", "female"),
        ),

        // ------------------------------------------------------------------
        // Economy — nama_10_gdp / prc_hicp_aind / gov_10dd_edpt1
        // ------------------------------------------------------------------
        IndicatorEntry(
            id = "gdp",
            label = "Gross domestic product (GDP)",
            module = SearchModule.ECONOMY,
            datasetCode = "nama_10_gdp",
            description = "GDP at current prices, million euro",
            keywords = listOf("gdp", "economy", "output", "growth", "wealth", "national accounts"),
        ),
        IndicatorEntry(
            id = "hicp-inflation",
            label = "HICP inflation",
            module = SearchModule.ECONOMY,
            datasetCode = "prc_hicp_aind",
            description = "Harmonised index of consumer prices, annual average",
            keywords = listOf("inflation", "prices", "hicp", "cost of living", "consumer prices", "cpi"),
        ),
        IndicatorEntry(
            id = "government-deficit",
            label = "Government deficit / surplus",
            module = SearchModule.ECONOMY,
            datasetCode = "gov_10dd_edpt1",
            description = "General government net lending/borrowing, % of GDP",
            keywords = listOf("deficit", "surplus", "budget", "fiscal", "government", "public finance", "maastricht"),
        ),

        // ------------------------------------------------------------------
        // Environment — env_air_gge / nrg_bal_c / sdg_13_10
        // ------------------------------------------------------------------
        IndicatorEntry(
            id = "ghg-total",
            label = "Greenhouse gas emissions",
            module = SearchModule.ENVIRONMENT,
            datasetCode = "env_air_gge",
            description = "Total GHG emissions, million tonnes CO2 equivalent",
            keywords = listOf("emissions", "ghg", "co2", "carbon", "greenhouse", "climate", "pollution"),
        ),
        IndicatorEntry(
            id = "ghg-transport",
            label = "Transport emissions",
            module = SearchModule.ENVIRONMENT,
            datasetCode = "env_air_gge",
            description = "GHG emissions from the transport sector (CRF 1.A.3)",
            keywords = listOf("emissions", "co2", "cars", "transport", "exhaust", "fuel"),
        ),
        IndicatorEntry(
            id = "ghg-industry",
            label = "Industry emissions",
            module = SearchModule.ENVIRONMENT,
            datasetCode = "env_air_gge",
            description = "GHG emissions from manufacturing and construction (CRF 1.A.2)",
            keywords = listOf("emissions", "co2", "industry", "manufacturing", "factories"),
        ),
        IndicatorEntry(
            id = "energy-consumption",
            label = "Final energy consumption",
            module = SearchModule.ENVIRONMENT,
            datasetCode = "nrg_bal_c",
            description = "Total, transport and industry final energy use, ktoe",
            keywords = listOf("energy", "consumption", "electricity", "fuel", "power", "ktoe"),
        ),
        IndicatorEntry(
            id = "climate-action-sdg13",
            label = "Climate action (SDG 13)",
            module = SearchModule.ENVIRONMENT,
            datasetCode = "sdg_13_10",
            description = "GHG emissions indexed to 1990 levels",
            keywords = listOf("sdg", "climate", "sustainability", "paris agreement", "1990", "targets"),
        ),

        // ------------------------------------------------------------------
        // Trade — ext_lt_intratrd
        // ------------------------------------------------------------------
        IndicatorEntry(
            id = "intra-eu-exports",
            label = "Intra-EU exports",
            module = SearchModule.TRADE,
            datasetCode = "ext_lt_intratrd",
            description = "Value of goods exported to other EU members, million euro",
            keywords = listOf("exports", "trade", "goods", "selling", "shipments"),
        ),
        IndicatorEntry(
            id = "intra-eu-imports",
            label = "Intra-EU imports",
            module = SearchModule.TRADE,
            datasetCode = "ext_lt_intratrd",
            description = "Value of goods imported from other EU members, million euro",
            keywords = listOf("imports", "trade", "goods", "buying", "purchases"),
        ),
        IndicatorEntry(
            id = "intra-eu-trade-balance",
            label = "Intra-EU trade balance",
            module = SearchModule.TRADE,
            datasetCode = "ext_lt_intratrd",
            description = "Net trade balance with other EU members, million euro",
            keywords = listOf("balance", "trade", "net exports", "deficit", "surplus"),
        ),

        // ------------------------------------------------------------------
        // Transport — road_pa_buscoa / avia_paoc
        // ------------------------------------------------------------------
        IndicatorEntry(
            id = "road-passengers",
            label = "Road passengers (bus & coach)",
            module = SearchModule.TRANSPORT,
            datasetCode = "road_pa_buscoa",
            description = "Passengers carried by buses and coaches",
            keywords = listOf("road", "bus", "coach", "passengers", "public transport", "travel"),
        ),
        IndicatorEntry(
            id = "air-passengers",
            label = "Air passengers",
            module = SearchModule.TRANSPORT,
            datasetCode = "avia_paoc",
            description = "Passengers carried on commercial flights",
            keywords = listOf("air", "flights", "aviation", "airports", "planes", "passengers", "airlines"),
        ),

        // ------------------------------------------------------------------
        // Tourism — tour_occ_ninat / tour_dem_tttot / tour_occ_nim
        // ------------------------------------------------------------------
        IndicatorEntry(
            id = "tourism-nights-domestic",
            label = "Nights spent — domestic tourists",
            module = SearchModule.TOURISM,
            datasetCode = "tour_occ_ninat",
            description = "Nights spent by residents in tourist accommodation",
            keywords = listOf("tourism", "nights", "hotels", "domestic", "accommodation", "staycation"),
        ),
        IndicatorEntry(
            id = "tourism-nights-foreign",
            label = "Nights spent — foreign tourists",
            module = SearchModule.TOURISM,
            datasetCode = "tour_occ_ninat",
            description = "Nights spent by non-residents in tourist accommodation",
            keywords = listOf("tourism", "tourists", "foreign", "international", "hotels", "visitors"),
        ),
        IndicatorEntry(
            id = "tourism-nights-total",
            label = "Nights spent — total",
            module = SearchModule.TOURISM,
            datasetCode = "tour_occ_ninat",
            description = "All nights spent in hotels and similar accommodation",
            keywords = listOf("tourism", "nights", "hotels", "occupancy", "accommodation"),
        ),
        IndicatorEntry(
            id = "tourism-trips",
            label = "Tourism trips",
            module = SearchModule.TOURISM,
            datasetCode = "tour_dem_tttot",
            description = "Trips of one night or more taken by residents",
            keywords = listOf("trips", "travel", "holidays", "vacation", "tourism demand", "journeys"),
        ),
        IndicatorEntry(
            id = "tourism-seasonality",
            label = "Tourism seasonality",
            module = SearchModule.TOURISM,
            datasetCode = "tour_occ_nim",
            description = "Monthly nights spent — the seasonality heatmap",
            keywords = listOf("seasonality", "monthly", "summer", "winter", "season", "peak", "heatmap"),
        ),

        // ------------------------------------------------------------------
        // Social — ilc_li02 / ilc_peps01n / hlth_silc_01
        // ------------------------------------------------------------------
        IndicatorEntry(
            id = "poverty-rate",
            label = "At-risk-of-poverty rate",
            module = SearchModule.SOCIAL,
            datasetCode = "ilc_li02",
            description = "Share of people below 60% of median equivalised income",
            keywords = listOf("poverty", "income", "inequality", "poor", "median income"),
        ),
        IndicatorEntry(
            id = "arope-rate",
            label = "At risk of poverty or social exclusion (AROPE)",
            module = SearchModule.SOCIAL,
            datasetCode = "ilc_peps01n",
            description = "People at risk of poverty or social exclusion, % of population",
            keywords = listOf("arope", "exclusion", "social", "deprivation", "poverty", "vulnerable"),
        ),
        IndicatorEntry(
            id = "health-satisfaction",
            label = "Self-perceived health",
            module = SearchModule.SOCIAL,
            datasetCode = "hlth_silc_01",
            description = "Share of adults rating their health as very good",
            keywords = listOf("health", "wellbeing", "satisfaction", "wellness", "quality of life"),
        ),

        // ------------------------------------------------------------------
        // Science — rd_e_gerdtot / isoc_ci_ifp_iu / edat_lfse_03
        // ------------------------------------------------------------------
        IndicatorEntry(
            id = "rd-intensity",
            label = "R&D expenditure",
            module = SearchModule.SCIENCE,
            datasetCode = "rd_e_gerdtot",
            description = "Gross domestic expenditure on R&D, % of GDP",
            keywords = listOf("research", "development", "r&d", "innovation", "science", "spending"),
        ),
        IndicatorEntry(
            id = "internet-use",
            label = "Daily internet use",
            module = SearchModule.SCIENCE,
            datasetCode = "isoc_ci_ifp_iu",
            description = "Share of individuals using the internet daily",
            keywords = listOf("internet", "digital", "online", "ict", "connectivity", "web"),
        ),
        IndicatorEntry(
            id = "tertiary-education",
            label = "Tertiary educational attainment",
            module = SearchModule.SCIENCE,
            datasetCode = "edat_lfse_03",
            description = "Share of 25–64 year olds with tertiary education",
            keywords = listOf("education", "university", "degree", "graduates", "tertiary", "college", "skills"),
        ),
    )
}
