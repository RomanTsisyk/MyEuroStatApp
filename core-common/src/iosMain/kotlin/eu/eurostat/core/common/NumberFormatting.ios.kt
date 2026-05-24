package eu.eurostat.core.common

import platform.Foundation.NSLocale
import platform.Foundation.NSNumber
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSNumberFormatterDecimalStyle
import platform.Foundation.NSNumberFormatterNoStyle
import platform.Foundation.currentLocale
import platform.Foundation.localeWithLocaleIdentifier
import kotlin.math.abs

/** iOS implementation using [NSNumberFormatter]. */
actual fun formatCompactNumber(value: Double, locale: String?): String {
    val nsLocale = locale?.let { NSLocale.localeWithLocaleIdentifier(it) }
        ?: NSLocale.currentLocale
    return manualCompact(value, nsLocale)
}

actual fun formatPercent(value: Double, fractionDigits: Int, locale: String?): String {
    val nsLocale = locale?.let { NSLocale.localeWithLocaleIdentifier(it) }
        ?: NSLocale.currentLocale
    val fmt = NSNumberFormatter().apply {
        this.locale = nsLocale
        numberStyle = NSNumberFormatterDecimalStyle
        minimumFractionDigits = fractionDigits.toULong()
        maximumFractionDigits = fractionDigits.toULong()
    }
    return "${fmt.stringFromNumber(NSNumber(double = value)) ?: value.toString()}%"
}

actual fun formatGrouped(value: Double, locale: String?): String {
    val nsLocale = locale?.let { NSLocale.localeWithLocaleIdentifier(it) }
        ?: NSLocale.currentLocale
    val fmt = NSNumberFormatter().apply {
        this.locale = nsLocale
        numberStyle = NSNumberFormatterDecimalStyle
        usesGroupingSeparator = true
        maximumFractionDigits = 0u
    }
    return fmt.stringFromNumber(NSNumber(double = value)) ?: value.toLong().toString()
}

private fun manualCompact(value: Double, nsLocale: NSLocale): String {
    val absVal = abs(value)
    val sign = if (value < 0) "-" else ""
    fun fmtDecimal(v: Double): String {
        val fmt = NSNumberFormatter().apply {
            locale = nsLocale
            numberStyle = NSNumberFormatterDecimalStyle
            maximumFractionDigits = 1u
            minimumFractionDigits = 0u
        }
        return fmt.stringFromNumber(NSNumber(double = v)) ?: v.toString()
    }
    return when {
        absVal >= 1_000_000_000 -> "${sign}${fmtDecimal(absVal / 1_000_000_000)}B"
        absVal >= 1_000_000     -> "${sign}${fmtDecimal(absVal / 1_000_000)}M"
        absVal >= 1_000         -> "${sign}${fmtDecimal(absVal / 1_000)}K"
        else -> {
            val fmt = NSNumberFormatter().apply {
                locale = nsLocale
                numberStyle = NSNumberFormatterNoStyle
                maximumFractionDigits = 1u
                minimumFractionDigits = 0u
            }
            fmt.stringFromNumber(NSNumber(double = value)) ?: value.toString()
        }
    }
}
