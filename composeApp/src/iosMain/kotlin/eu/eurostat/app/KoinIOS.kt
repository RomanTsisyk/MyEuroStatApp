package eu.eurostat.app

import eu.eurostat.app.di.appModules
import eu.eurostat.core.database.DatabaseDriverFactory
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.koin.mp.KoinPlatform

fun initKoinIos() {
    if (KoinPlatform.getKoinOrNull() != null) return
    startKoin {
        modules(
            appModules() + module {
                single { DatabaseDriverFactory() }
            }
        )
    }
}
