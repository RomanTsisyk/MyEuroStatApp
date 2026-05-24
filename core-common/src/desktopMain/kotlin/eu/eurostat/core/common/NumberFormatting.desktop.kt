package eu.eurostat.core.common

import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

/** Desktop (JVM) implementation using [java.text.NumberFormat]. */
actual fun formatCompactNumber(value: Double, locale: String?): String {
    val javaLocale = if (locale != null) Locale.forLanguageTag(locale) else Locale.getDefault()
    // CompactNumberFormat is available on JDK 12+; use manual fallback for broader compat
    return manualCompact(value, javaLocale)
}

actual fun formatPercent(value: Double, fractionDigits: Int, locale: String?): String {
    val javaLocale = if (locale != null) Locale.forLanguageTag(locale) else Locale.getDefault()
    val fmt = NumberFormat.getNumberInstance(javaLocale)
    fmt.minimumFractionDigits = fractionDigits
    fmt.maximumFractionDigits = fractionDigits
    fmt.roundingMode = RoundingMode.HALF_UP
    return "${fmt.format(value)}%"
}

actual fun formatGrouped(value: Double, locale: String?): String {
    val javaLocale = if (locale != null) Locale.forLanguageTag(locale) else Locale.getDefault()
    val fmt = NumberFormat.getNumberInstance(javaLocale)
    fmt.isGroupingUsed = true
    fmt.maximumFractionDigits = 0
    fmt.roundingMode = RoundingMode.HALF_UP
    return fmt.format(value)
}

private fun manualCompact(value: Double, locale: Locale): String {
    val abs = kotlin.math.abs(value)
    val sign = if (value < 0) "-" else ""
    val fmt = NumberFormat.getNumberInstance(locale).apply {
        maximumFractionDigits = 1
        minimumFractionDigits = 0
    }
    return when {
        abs >= 1_000_000_000 -> "${sign}${fmt.format(abs / 1_000_000_000)}B"
        abs >= 1_000_000     -> "${sign}${fmt.format(abs / 1_000_000)}M"
        abs >= 1_000         -> "${sign}${fmt.format(abs / 1_000)}K"
        else                 -> NumberFormat.getNumberInstance(locale).apply {
            maximumFractionDigits = 1; minimumFractionDigits = 0
        }.format(value)
    }
}
