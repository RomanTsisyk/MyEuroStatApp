package eu.eurostat.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import myeurostatapp.core_ui.generated.resources.Res
import myeurostatapp.core_ui.generated.resources.ibmplexmono_medium
import myeurostatapp.core_ui.generated.resources.ibmplexmono_regular
import myeurostatapp.core_ui.generated.resources.inter_bold
import myeurostatapp.core_ui.generated.resources.inter_medium
import myeurostatapp.core_ui.generated.resources.inter_regular
import myeurostatapp.core_ui.generated.resources.inter_semibold
import myeurostatapp.core_ui.generated.resources.inter_tight_bold
import myeurostatapp.core_ui.generated.resources.inter_tight_medium
import myeurostatapp.core_ui.generated.resources.inter_tight_semibold
import org.jetbrains.compose.resources.Font

/**
 * Typography scale for the Eurostat design system.
 *
 * Uses bundled Inter (Regular/Medium/SemiBold/Bold) for body/UI text,
 * Inter Tight (Medium/SemiBold/Bold) for large display headlines, and
 * IBM Plex Mono (Regular/Medium) for tabular numerals. All fonts are loaded
 * via Compose Multiplatform Resources from
 * `core-ui/src/commonMain/composeResources/font/`.
 *
 * Inter Tight is sourced from Google Fonts (SIL OFL 1.1 license).
 *
 * Tabular styles enable the `tnum` OpenType feature so digits align in
 * columns inside tables, KPIs and chart axes.
 */
data class EurostatTypography(
    val displayLarge: TextStyle,
    val displayMedium: TextStyle,
    val headlineLarge: TextStyle,
    val headlineSmall: TextStyle,
    val bodyLarge: TextStyle,
    val bodyMedium: TextStyle,
    val bodySmall: TextStyle,
    val labelLarge: TextStyle,
    val labelSmall: TextStyle,
    val eyebrow: TextStyle,
    val tabularNumLarge: TextStyle,
    val tabularNumSmall: TextStyle,
)

/**
 * Fallback typography that uses platform system fonts.
 * Used as the static default for [LocalEurostatTypography] so that
 * composition locals can be initialised outside a composable context.
 */
internal fun defaultTypography(): EurostatTypography =
    buildTypography(FontFamily.SansSerif, FontFamily.SansSerif, FontFamily.Monospace)

/**
 * Typography backed by the bundled Inter + IBM Plex Mono fonts.
 * Must be called from a [Composable] context (inside [EurostatTheme]).
 */
@Composable
internal fun bundledTypography(): EurostatTypography {
    val sans = FontFamily(
        Font(Res.font.inter_regular, weight = FontWeight.Normal),
        Font(Res.font.inter_medium, weight = FontWeight.Medium),
        Font(Res.font.inter_semibold, weight = FontWeight.SemiBold),
        Font(Res.font.inter_bold, weight = FontWeight.Bold),
    )
    val tight = FontFamily(
        Font(Res.font.inter_tight_medium, weight = FontWeight.Medium),
        Font(Res.font.inter_tight_semibold, weight = FontWeight.SemiBold),
        Font(Res.font.inter_tight_bold, weight = FontWeight.Bold),
    )
    val mono = FontFamily(
        Font(Res.font.ibmplexmono_regular, weight = FontWeight.Normal),
        Font(Res.font.ibmplexmono_medium, weight = FontWeight.Medium),
    )
    return buildTypography(sans, tight, mono)
}

private fun buildTypography(sans: FontFamily, tight: FontFamily, mono: FontFamily): EurostatTypography =
    EurostatTypography(
        displayLarge = TextStyle(
            fontFamily = tight,
            fontSize = 40.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-1.4).sp,
            lineHeight = 40.sp,
        ),
        displayMedium = TextStyle(
            fontFamily = tight,
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.8).sp,
            lineHeight = 36.sp,
        ),
        headlineLarge = TextStyle(
            fontFamily = sans,
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.4).sp,
        ),
        headlineSmall = TextStyle(
            fontFamily = sans,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.2).sp,
        ),
        bodyLarge = TextStyle(
            fontFamily = sans,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
        ),
        bodyMedium = TextStyle(
            fontFamily = sans,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
        ),
        bodySmall = TextStyle(
            fontFamily = sans,
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
        ),
        labelLarge = TextStyle(
            fontFamily = sans,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        ),
        labelSmall = TextStyle(
            fontFamily = sans,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.1.sp,
        ),
        eyebrow = TextStyle(
            fontFamily = tight,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.2.sp,
        ),
        tabularNumLarge = TextStyle(
            fontFamily = sans,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.8).sp,
            fontFeatureSettings = "tnum",
        ),
        tabularNumSmall = TextStyle(
            fontFamily = mono,
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            fontFeatureSettings = "tnum",
        ),
    )

