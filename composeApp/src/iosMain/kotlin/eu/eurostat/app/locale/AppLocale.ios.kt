package eu.eurostat.app.locale

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.staticCompositionLocalOf
import platform.Foundation.NSLocale
import platform.Foundation.NSUserDefaults
import platform.Foundation.preferredLanguages

/**
 * iOS: Compose Resources resolves strings through `NSLocale.preferredLanguages`,
 * which honours the `AppleLanguages` override in [NSUserDefaults]. Writing the
 * override and forcing recomposition (the `key(...)` in `AppLocaleEnvironment`)
 * switches the UI language in place.
 *
 * `default` is initialized lazily: Kotlin/Native fails the whole file if a
 * non-trivial property initializer throws during static init (see CLAUDE.md's
 * `\p{L}` regex incident).
 */
actual object LocalAppLocale {

    private const val LANG_KEY = "AppleLanguages"

    /** System language captured before the first override, restored on `null`. */
    private val default: String by lazy {
        NSLocale.preferredLanguages.firstOrNull() as? String ?: "en"
    }

    private val LocalAppLocaleState = staticCompositionLocalOf { default }

    actual val current: String
        @Composable get() = LocalAppLocaleState.current

    @Composable
    actual infix fun provides(value: String?): ProvidedValue<*> {
        val new = value ?: default
        if (value == null) {
            NSUserDefaults.standardUserDefaults.removeObjectForKey(LANG_KEY)
        } else {
            NSUserDefaults.standardUserDefaults.setObject(arrayListOf(new), LANG_KEY)
        }
        return LocalAppLocaleState.provides(new)
    }
}
