package eu.eurostat.ui.format

import platform.Foundation.NSLocale
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSNumberFormatterDecimalStyle
import platform.Foundation.currentLocale

/**
 * iOS actual: reads separators from an [NSNumberFormatter] seeded with
 * `NSLocale.currentLocale`, mirroring the device's Settings > Language & Region.
 */
private val deviceFormatter: NSNumberFormatter by lazy {
    NSNumberFormatter().apply {
        locale = NSLocale.currentLocale
        numberStyle = NSNumberFormatterDecimalStyle
    }
}

internal actual fun localeDecimalSeparator(): Char =
    deviceFormatter.decimalSeparator.firstOrNull() ?: '.'

internal actual fun localeGroupingSeparator(): Char =
    deviceFormatter.groupingSeparator.firstOrNull() ?: ','
