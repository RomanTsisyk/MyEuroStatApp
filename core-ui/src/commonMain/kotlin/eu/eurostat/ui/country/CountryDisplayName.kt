package eu.eurostat.ui.country

import androidx.compose.runtime.Composable
import myeurostatapp.core_ui.generated.resources.Res
import myeurostatapp.core_ui.generated.resources.country_at
import myeurostatapp.core_ui.generated.resources.country_be
import myeurostatapp.core_ui.generated.resources.country_bg
import myeurostatapp.core_ui.generated.resources.country_ch
import myeurostatapp.core_ui.generated.resources.country_cy
import myeurostatapp.core_ui.generated.resources.country_cz
import myeurostatapp.core_ui.generated.resources.country_de
import myeurostatapp.core_ui.generated.resources.country_dk
import myeurostatapp.core_ui.generated.resources.country_ea20
import myeurostatapp.core_ui.generated.resources.country_ee
import myeurostatapp.core_ui.generated.resources.country_el
import myeurostatapp.core_ui.generated.resources.country_es
import myeurostatapp.core_ui.generated.resources.country_eu27_2020
import myeurostatapp.core_ui.generated.resources.country_fi
import myeurostatapp.core_ui.generated.resources.country_fr
import myeurostatapp.core_ui.generated.resources.country_hr
import myeurostatapp.core_ui.generated.resources.country_hu
import myeurostatapp.core_ui.generated.resources.country_ie
import myeurostatapp.core_ui.generated.resources.country_is
import myeurostatapp.core_ui.generated.resources.country_it
import myeurostatapp.core_ui.generated.resources.country_lt
import myeurostatapp.core_ui.generated.resources.country_lu
import myeurostatapp.core_ui.generated.resources.country_lv
import myeurostatapp.core_ui.generated.resources.country_mt
import myeurostatapp.core_ui.generated.resources.country_nl
import myeurostatapp.core_ui.generated.resources.country_no
import myeurostatapp.core_ui.generated.resources.country_pl
import myeurostatapp.core_ui.generated.resources.country_pt
import myeurostatapp.core_ui.generated.resources.country_ro
import myeurostatapp.core_ui.generated.resources.country_se
import myeurostatapp.core_ui.generated.resources.country_si
import myeurostatapp.core_ui.generated.resources.country_sk
import myeurostatapp.core_ui.generated.resources.country_tr
import myeurostatapp.core_ui.generated.resources.country_uk
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Resolves a Eurostat country/area [code] to its localized display name for the
 * current app locale (English, Polish or Ukrainian).
 *
 * The English names in `eu.eurostat.core.common.EurostatCountries` and in the
 * Eurostat API labels stay the data-layer default; this function is the display
 * boundary that swaps them for translated names. Call it wherever a country name
 * is rendered as text, never in the data or domain layers.
 *
 * @param code Eurostat country/area code (e.g. `"DE"`, `"EL"`, `"EU27_2020"`).
 *   Matching is exact and case-sensitive, like `EurostatCountries.byCode`.
 * @param fallback returned verbatim when [code] has no bundled translation
 *   (typically the English name already carried by the data, or the code itself).
 */
@Composable
fun countryDisplayName(code: String, fallback: String): String {
    val res = countryNameRes(code)
    return if (res != null) stringResource(res) else fallback
}

/**
 * Maps a Eurostat country/area [code] to the string resource holding its display
 * name (`country_<lowercase code>` in core-ui's `composeResources`), or `null`
 * when the code is not one of the codes in `EurostatCountries.ALL`.
 *
 * Compose Resources has no lookup-by-name at runtime, so every code is mapped
 * explicitly. Kept non-composable so the mapping can be unit-tested.
 */
internal fun countryNameRes(code: String): StringResource? = when (code) {
    "EU27_2020" -> Res.string.country_eu27_2020
    "EA20" -> Res.string.country_ea20
    "AT" -> Res.string.country_at
    "BE" -> Res.string.country_be
    "BG" -> Res.string.country_bg
    "HR" -> Res.string.country_hr
    "CY" -> Res.string.country_cy
    "CZ" -> Res.string.country_cz
    "DK" -> Res.string.country_dk
    "EE" -> Res.string.country_ee
    "FI" -> Res.string.country_fi
    "FR" -> Res.string.country_fr
    "DE" -> Res.string.country_de
    "EL" -> Res.string.country_el
    "HU" -> Res.string.country_hu
    "IE" -> Res.string.country_ie
    "IT" -> Res.string.country_it
    "LV" -> Res.string.country_lv
    "LT" -> Res.string.country_lt
    "LU" -> Res.string.country_lu
    "MT" -> Res.string.country_mt
    "NL" -> Res.string.country_nl
    "PL" -> Res.string.country_pl
    "PT" -> Res.string.country_pt
    "RO" -> Res.string.country_ro
    "SK" -> Res.string.country_sk
    "SI" -> Res.string.country_si
    "ES" -> Res.string.country_es
    "SE" -> Res.string.country_se
    "IS" -> Res.string.country_is
    "NO" -> Res.string.country_no
    "CH" -> Res.string.country_ch
    "UK" -> Res.string.country_uk
    "TR" -> Res.string.country_tr
    else -> null
}
