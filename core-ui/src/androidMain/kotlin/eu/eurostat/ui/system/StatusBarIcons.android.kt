package eu.eurostat.ui.system

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Android actual: flips the window's light-status-bar appearance flag while
 * composed and puts the previous value back on dispose. The activity enables
 * edge-to-edge once in `onCreate` (which picks the initial value from the
 * system theme) and never touches the flag again, so this override is what
 * the user sees until the composable leaves the composition.
 */
@Composable
actual fun StatusBarIcons(light: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    DisposableEffect(view, light) {
        val window = view.context.findActivity()?.window
        if (window == null) {
            onDispose { }
        } else {
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            val previous = controller.isAppearanceLightStatusBars
            // "Light status bars" means dark icons, hence the inversion.
            controller.isAppearanceLightStatusBars = !light
            onDispose { controller.isAppearanceLightStatusBars = previous }
        }
    }
}

/** Unwraps [ContextWrapper]s (themed / lifecycle contexts) down to the hosting [Activity], if any. */
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
