package eu.eurostat.ui.format

import java.text.DecimalFormatSymbols

/** Android actual: reads separators from the device's default [DecimalFormatSymbols]. */
internal actual fun localeDecimalSeparator(): Char =
    DecimalFormatSymbols.getInstance().decimalSeparator

internal actual fun localeGroupingSeparator(): Char =
    DecimalFormatSymbols.getInstance().groupingSeparator
