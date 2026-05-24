package eu.eurostat.core.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import eu.eurostat.core.database.generated.AppDatabase

actual class DatabaseDriverFactory {
    actual fun create(): SqlDriver =
        NativeSqliteDriver(AppDatabase.Schema, "eurostat.db")
}
