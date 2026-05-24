package eu.eurostat.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.CanvasBasedWindow

/**
 * Compose Web (wasmJs) entry point.
 *
 * Full feature functionality (navigation, SQLDelight cache, feature screens) requires
 * SQLDelight wasmJs support — tracked in CLAUDE.md Phase 5 TODOs.
 * For now this renders a minimal loading placeholder so the wasmJs target compiles
 * and can be served via `./gradlew :composeApp:wasmJsBrowserDevelopmentRun`.
 */
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    CanvasBasedWindow(canvasElementId = "ComposeTarget") {
        EurostatWebApp()
    }
}

@Composable
fun EurostatWebApp() {
    MaterialTheme {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text("Eurostat — Web preview coming soon")
        }
    }
}
