package eu.eurostat.app

import android.app.Application
import eu.eurostat.app.di.appModules
import eu.eurostat.core.database.DatabaseDriverFactory
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.dsl.module

class EurostatApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (GlobalContext.getOrNull() != null) return
        startKoin {
            androidContext(this@EurostatApplication)
            modules(
                appModules() + module {
                    single { DatabaseDriverFactory(get()) }
                }
            )
        }
    }
}
