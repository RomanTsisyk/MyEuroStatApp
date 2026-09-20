package eu.eurostat.ui.system

import androidx.compose.runtime.Composable

/** iOS actual: no-op — the status bar style is owned by the hosting view controller. */
@Composable
@Suppress("UNUSED_PARAMETER")
actual fun StatusBarIcons(light: Boolean) {
    // Intentionally empty.
}
