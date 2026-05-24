package eu.eurostat.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalEurostatColors = staticCompositionLocalOf { lightColors() }
private val LocalEurostatTypography = staticCompositionLocalOf { defaultTypography() }
private val LocalEurostatSpacing = staticCompositionLocalOf { EurostatSpacing() }
private val LocalEurostatShapes = staticCompositionLocalOf { EurostatShapes() }
private val LocalModuleAccents = staticCompositionLocalOf { lightModuleAccents() }
private val LocalIsDark = compositionLocalOf { false }

/**
 * Root theme composable. Wraps [MaterialTheme] with a colour scheme matched
 * to the active Eurostat palette and exposes the design tokens via the [Euro]
 * accessor object.
 *
 * @param darkTheme whether to apply the dark palette; defaults to the
 *   system setting via [isSystemInDarkTheme].
 */
@Composable
fun EurostatTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) darkColors() else lightColors()
    val typography = bundledTypography()
    val spacing = EurostatSpacing()
    val shapes = EurostatShapes()
    val modules = if (darkTheme) darkModuleAccents() else lightModuleAccents()

    val materialScheme = if (darkTheme) {
        darkColorScheme(
            primary = colors.accent,
            onPrimary = colors.paper,
            secondary = colors.muted,
            onSecondary = colors.paper,
            background = colors.paper,
            onBackground = colors.ink,
            surface = colors.paperAlt,
            onSurface = colors.ink,
            surfaceVariant = colors.paperAlt,
            onSurfaceVariant = colors.inkSubtle,
            error = colors.warn,
            onError = colors.paper,
            outline = colors.muted,
        )
    } else {
        lightColorScheme(
            primary = colors.accent,
            onPrimary = colors.paper,
            secondary = colors.muted,
            onSecondary = colors.paper,
            background = colors.paper,
            onBackground = colors.ink,
            surface = colors.paperAlt,
            onSurface = colors.ink,
            surfaceVariant = colors.paperAlt,
            onSurfaceVariant = colors.inkSubtle,
            error = colors.warn,
            onError = colors.paper,
            outline = colors.muted,
        )
    }

    CompositionLocalProvider(
        LocalEurostatColors provides colors,
        LocalEurostatTypography provides typography,
        LocalEurostatSpacing provides spacing,
        LocalEurostatShapes provides shapes,
        LocalModuleAccents provides modules,
        LocalIsDark provides darkTheme,
    ) {
        MaterialTheme(
            colorScheme = materialScheme,
            content = content,
        )
    }
}

/**
 * Token accessor for the active Eurostat theme. Call sites use
 * `Euro.colors.accent`, `Euro.spacing.base`, etc. — all accessors are
 * `@ReadOnlyComposable` so they can be invoked from any composable context
 * including default parameter values.
 */
object Euro {
    val colors: EurostatColors
        @Composable @ReadOnlyComposable get() = LocalEurostatColors.current
    val typography: EurostatTypography
        @Composable @ReadOnlyComposable get() = LocalEurostatTypography.current
    val spacing: EurostatSpacing
        @Composable @ReadOnlyComposable get() = LocalEurostatSpacing.current
    val shapes: EurostatShapes
        @Composable @ReadOnlyComposable get() = LocalEurostatShapes.current
    val moduleAccents: ModuleAccents
        @Composable @ReadOnlyComposable get() = LocalModuleAccents.current
    val isDark: Boolean
        @Composable @ReadOnlyComposable get() = LocalIsDark.current
    val platform: EuroPlatform
        @Composable @ReadOnlyComposable get() = LocalEuroPlatform.current
}
