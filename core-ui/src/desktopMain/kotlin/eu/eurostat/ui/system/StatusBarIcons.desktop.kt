package eu.eurostat.ui.system

import androidx.compose.runtime.Composable

/** Desktop actual: no-op — desktop windows have no status bar. */
@Composable
@Suppress("UNUSED_PARAMETER")
actual fun StatusBarIcons(light: Boolean) {
    // Intentionally empty.
}
