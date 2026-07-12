package eu.eurostat.app.locale

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.key

/**
 * Platform hook that overrides the locale Compose Resources' `stringResource(...)`
 * resolves against, so the language picked on the Settings screen takes effect
 * app-wide without an OS-level locale change.
 *
 * Pattern from the official Compose Multiplatform "Manage local resource
 * environment" guide: each platform installs the override where its resource
 * loader actually looks it up (`java.util.Locale` default + Android
 * `Configuration` on Android, `java.util.Locale` default on desktop,
 * `AppleLanguages` in `NSUserDefaults` on iOS).
 */
expect object LocalAppLocale {

    /** The language tag currently in effect (e.g. `"en"`, `"pl"`, `"uk"`). */
    val current: String
        @Composable get

    /**
     * Installs [value] as the app-wide locale override, or restores the system
     * locale when [value] is null. Must be applied via [CompositionLocalProvider]
     * (see [AppLocaleEnvironment]).
     */
    @Composable
    infix fun provides(value: String?): ProvidedValue<*>
}

/**
 * Wraps [content] so every `stringResource(...)` beneath it resolves against
 * [languageTag] (`"en"` / `"pl"` / `"uk"`), or the system locale when null.
 *
 * `key(languageTag)` intentionally discards and rebuilds the whole content
 * subtree on a language switch: string resources are read during composition,
 * so already-composed screens would otherwise keep their old-language text
 * until something else invalidated them.
 */
@Composable
fun AppLocaleEnvironment(languageTag: String?, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalAppLocale provides languageTag) {
        key(languageTag) {
            content()
        }
    }
}
