package eu.eurostat.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.eurostat.core.common.flagFor
import myeurostatapp.core_ui.generated.resources.*
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/**
 * Renders a country's flag as a bundled circular vector icon, falling back to
 * the Unicode flag emoji (via [flagFor]) for codes with no bundled artwork.
 *
 * ## Icon set
 * Flags are sourced from [HatScripts/circle-flags](https://github.com/HatScripts/circle-flags)
 * (MIT license). Each SVG was converted to an Android vector drawable and bundled
 * under `core-ui/src/commonMain/composeResources/drawable/flag_<eurostat_code>.xml`
 * (lowercase Eurostat code, e.g. `flag_de.xml`, `flag_el.xml`, `flag_eu27_2020.xml`).
 * The circle-flags source SVGs clip their artwork with an internal `<mask>`, which
 * Android vector drawables cannot express; that mask was stripped before conversion
 * and the circular crop is instead applied here via `Modifier.clip(CircleShape)`.
 *
 * [flagDrawableFor] covers every code [eu.eurostat.core.common.EurostatCountries.ALL]
 * can resolve to a real flag for: the 27 EU member states, the EU aggregate
 * (`EU27_2020`), the EFTA states the app shows (`IS`, `NO`, `CH`), the UK (Eurostat
 * code `UK`, drawn from circle-flags' `gb` artwork) and Türkiye (`TR`) — honoring the
 * same Eurostat-vs-ISO quirks as [flagFor] (`EL` = Greece, `UK` = United Kingdom).
 *
 * ## Fallback
 * Aggregate codes with no flag of their own (e.g. `EA20`, the euro-area aggregate,
 * which has no country flag) and any other unmapped code fall back to [flagFor]'s
 * Unicode emoji (or plain text, e.g. `"EA"`), rendered as [Text] so the UI never
 * shows a blank space.
 *
 * @param code Eurostat country/area code (e.g. `"DE"`, `"EL"`, `"EU27_2020"`).
 * @param modifier applied to the outer element (the image, or the fallback text).
 * @param size diameter of the rendered flag circle; ignored for the text fallback.
 */
@Composable
fun CountryFlag(
    code: String,
    modifier: Modifier = Modifier,
    size: Dp = 18.dp,
) {
    val drawable = flagDrawableFor(code)
    if (drawable != null) {
        Image(
            painter = painterResource(drawable),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(CircleShape),
        )
    } else {
        Text(
            text = flagFor(code),
            fontSize = 14.sp,
            lineHeight = 14.sp,
            modifier = modifier,
        )
    }
}

/**
 * Resolves a Eurostat country/area [code] to its bundled circle-flags
 * [DrawableResource], or `null` if no artwork is bundled for it (e.g. the `EA20`
 * euro-area aggregate, or any code outside [eu.eurostat.core.common.EurostatCountries.ALL]).
 */
private fun flagDrawableFor(code: String): DrawableResource? = when (code.uppercase()) {
    "EU27_2020", "EU28", "EU27", "EU" -> Res.drawable.flag_eu27_2020
    "AT" -> Res.drawable.flag_at
    "BE" -> Res.drawable.flag_be
    "BG" -> Res.drawable.flag_bg
    "HR" -> Res.drawable.flag_hr
    "CY" -> Res.drawable.flag_cy
    "CZ" -> Res.drawable.flag_cz
    "DK" -> Res.drawable.flag_dk
    "EE" -> Res.drawable.flag_ee
    "FI" -> Res.drawable.flag_fi
    "FR" -> Res.drawable.flag_fr
    "DE" -> Res.drawable.flag_de
    "EL", "GR" -> Res.drawable.flag_el
    "HU" -> Res.drawable.flag_hu
    "IE" -> Res.drawable.flag_ie
    "IT" -> Res.drawable.flag_it
    "LV" -> Res.drawable.flag_lv
    "LT" -> Res.drawable.flag_lt
    "LU" -> Res.drawable.flag_lu
    "MT" -> Res.drawable.flag_mt
    "NL" -> Res.drawable.flag_nl
    "PL" -> Res.drawable.flag_pl
    "PT" -> Res.drawable.flag_pt
    "RO" -> Res.drawable.flag_ro
    "SK" -> Res.drawable.flag_sk
    "SI" -> Res.drawable.flag_si
    "ES" -> Res.drawable.flag_es
    "SE" -> Res.drawable.flag_se
    "IS" -> Res.drawable.flag_is
    "NO" -> Res.drawable.flag_no
    "CH" -> Res.drawable.flag_ch
    "UK", "GB" -> Res.drawable.flag_uk
    "TR" -> Res.drawable.flag_tr
    else -> null
}
