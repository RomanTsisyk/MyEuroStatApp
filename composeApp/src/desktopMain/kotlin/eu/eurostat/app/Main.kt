package eu.eurostat.app

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import eu.eurostat.app.di.appModules
import eu.eurostat.core.database.DatabaseDriverFactory
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.dsl.module

fun main() {
    // Koin must be started before any component is created.
    if (GlobalContext.getOrNull() == null) {
        startKoin {
            modules(
                appModules() + module {
                    single { DatabaseDriverFactory() }
                }
            )
        }
    }

    // Run inside application{} so that Dispatchers.Main (Swing event loop)
    // is available when Decompose components start launching coroutines.
    application {
        val lifecycle = LifecycleRegistry()
        val root = createRootComponent(DefaultComponentContext(lifecycle))

        Window(
            onCloseRequest = ::exitApplication,
            title = "MyEuroStatApp",
        ) {
            EurostatApp(root)
        }
    }
}
