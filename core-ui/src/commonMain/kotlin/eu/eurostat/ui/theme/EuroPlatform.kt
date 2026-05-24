package eu.eurostat.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Identifies the host platform so that the design system can render
 * platform-appropriate UI variants (e.g. iOS frosted-glass bars vs.
 * Android M3 Expressive solid surfaces).
 *
 * Provided via [LocalEuroPlatform] in each platform's Compose root before
 * [EurostatTheme] is entered. Defaults to [Android] so that existing
 * call sites remain unaffected if no provider is present.
 */
enum class EuroPlatform { Android, Ios }

/**
 * Composition local that holds the current [EuroPlatform].
 *
 * Provide it in each platform's Compose root entry point:
 * ```kotlin
 * CompositionLocalProvider(LocalEuroPlatform provides EuroPlatform.Ios) { … }
 * ```
 */
val LocalEuroPlatform: ProvidableCompositionLocal<EuroPlatform> =
    staticCompositionLocalOf { EuroPlatform.Android }

// Euro.platform convenience accessor is added in EurostatTheme.kt
internal val currentPlatform: EuroPlatform
    @Composable @ReadOnlyComposable get() = LocalEuroPlatform.current
