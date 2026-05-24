package eu.eurostat.core.common

/**
 * A Eurostat country or aggregate area.
 *
 * @property code Eurostat country code (may differ from ISO 3166-1 alpha-2;
 *   e.g. `"EL"` for Greece, `"UK"` for United Kingdom).
 * @property name Human-readable English country name.
 * @property flag Unicode flag emoji for the country, computed from [flagFor].
 *   Aggregate areas (EU, EA) fall back to text symbols.
 */
data class EurostatCountry(
    val code: String,
    val name: String,
    val flag: String = flagFor(code),
)

/** Pre-built list of all Eurostat member states and aggregate areas. */
object EurostatCountries {
    val ALL: List<EurostatCountry> = listOf(
        EurostatCountry("EU27_2020", "EU (27)"),
        EurostatCountry("EA20", "Euro area (20)"),
        EurostatCountry("AT", "Austria"),
        EurostatCountry("BE", "Belgium"),
        EurostatCountry("BG", "Bulgaria"),
        EurostatCountry("HR", "Croatia"),
        EurostatCountry("CY", "Cyprus"),
        EurostatCountry("CZ", "Czechia"),
        EurostatCountry("DK", "Denmark"),
        EurostatCountry("EE", "Estonia"),
        EurostatCountry("FI", "Finland"),
        EurostatCountry("FR", "France"),
        EurostatCountry("DE", "Germany"),
        EurostatCountry("EL", "Greece"),
        EurostatCountry("HU", "Hungary"),
        EurostatCountry("IE", "Ireland"),
        EurostatCountry("IT", "Italy"),
        EurostatCountry("LV", "Latvia"),
        EurostatCountry("LT", "Lithuania"),
        EurostatCountry("LU", "Luxembourg"),
        EurostatCountry("MT", "Malta"),
        EurostatCountry("NL", "Netherlands"),
        EurostatCountry("PL", "Poland"),
        EurostatCountry("PT", "Portugal"),
        EurostatCountry("RO", "Romania"),
        EurostatCountry("SK", "Slovakia"),
        EurostatCountry("SI", "Slovenia"),
        EurostatCountry("ES", "Spain"),
        EurostatCountry("SE", "Sweden"),
        // EFTA / others commonly in Eurostat:
        EurostatCountry("IS", "Iceland"),
        EurostatCountry("NO", "Norway"),
        EurostatCountry("CH", "Switzerland"),
        EurostatCountry("UK", "United Kingdom"),
        EurostatCountry("TR", "Türkiye"),
    )

    fun byCode(code: String): EurostatCountry? = ALL.firstOrNull { it.code == code }
}
