package eu.eurostat.core.common

/**
 * Formats [value] as a compact human-readable number with a K/M/B suffix.
 *
 * Examples (locale = null → default locale):
 * - `1_234_567.0` → `"1.2M"`
 * - `850_000.0`   → `"850K"`
 * - `12.3`        → `"12.3"`
 *
 * @param value the number to format.
 * @param locale BCP-47 locale tag (e.g. `"en"`, `"de"`, `"fr"`); `null` uses the device locale.
 */
expect fun formatCompactNumber(value: Double, locale: String? = null): String

/**
 * Formats [value] as a percentage string.
 *
 * The input is treated as an already-multiplied percentage (e.g. `12.5` → `"12.5%"`),
 * **not** as a fraction (pass `0.125` if you want `"12.5%"` from fraction form, but
 * you must multiply by 100 first).
 *
 * @param value percentage value (e.g. `12.5` for 12.5 %).
 * @param fractionDigits number of decimal places in the output.
 * @param locale BCP-47 locale tag; `null` uses the device locale.
 */
expect fun formatPercent(value: Double, fractionDigits: Int = 1, locale: String? = null): String

/**
 * Formats [value] with thousand-group separators, using the locale's grouping
 * and decimal conventions.
 *
 * Examples:
 * - US: `1_234_567.0` → `"1,234,567"`
 * - DE: `1_234_567.0` → `"1.234.567"`
 *
 * @param value the number to format.
 * @param locale BCP-47 locale tag; `null` uses the device locale.
 */
expect fun formatGrouped(value: Double, locale: String? = null): String
