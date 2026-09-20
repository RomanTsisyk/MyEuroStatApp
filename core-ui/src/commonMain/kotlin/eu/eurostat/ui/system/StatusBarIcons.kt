package eu.eurostat.ui.system

import androidx.compose.runtime.Composable

/**
 * Declares the tint of the system status-bar icons (clock, battery, signal)
 * for as long as this composable stays in the composition.
 *
 * Edge-to-edge screens draw their own colour behind the status bar, so the
 * icon tint has to match *that* colour, not the system theme: a near-black
 * header needs light icons, a light header needs dark ones. Pass the tint the
 * background behind the bar calls for, typically
 * `light = backgroundColor.luminance() < 0.5f`.
 *
 * Platform behaviour:
 * - Android: sets `WindowInsetsControllerCompat.isAppearanceLightStatusBars`
 *   (which is `!light`) on entering the composition and restores the value
 *   that was in effect before when it leaves, so screens that do not call this
 *   keep the default tint chosen by the activity.
 * - iOS and desktop: no-op (the status bar style is not controlled from here).
 *
 * @param light `true` for light (white) icons, to sit on a dark background;
 *   `false` for dark icons, to sit on a light background.
 */
@Composable
expect fun StatusBarIcons(light: Boolean)
