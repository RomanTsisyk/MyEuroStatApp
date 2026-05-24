package eu.eurostat.app

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.window.ComposeUIViewController
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.resume
import eu.eurostat.ui.theme.EuroPlatform
import eu.eurostat.ui.theme.LocalEuroPlatform
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    initKoinIos()
    val lifecycle = LifecycleRegistry()
    val root = createRootComponent(DefaultComponentContext(lifecycle = lifecycle))
    lifecycle.resume()
    return ComposeUIViewController {
        CompositionLocalProvider(LocalEuroPlatform provides EuroPlatform.Ios) {
            EurostatApp(root)
        }
    }
}
