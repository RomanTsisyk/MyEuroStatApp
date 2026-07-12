package eu.eurostat.ui.format

import java.text.DecimalFormatSymbols

/** Desktop (JVM) actual: reads separators from the JVM's default [DecimalFormatSymbols]. */
internal actual fun localeDecimalSeparator(): Char =
    DecimalFormatSymbols.getInstance().decimalSeparator

internal actual fun localeGroupingSeparator(): Char =
    DecimalFormatSymbols.getInstance().groupingSeparator
