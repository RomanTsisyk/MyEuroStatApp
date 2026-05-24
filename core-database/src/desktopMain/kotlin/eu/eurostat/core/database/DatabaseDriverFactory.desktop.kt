package eu.eurostat.core.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import eu.eurostat.core.database.generated.AppDatabase

actual class DatabaseDriverFactory {
    actual fun create(): SqlDriver {
        val driver = JdbcSqliteDriver("jdbc:sqlite:eurostat.db")
        AppDatabase.Schema.create(driver)
        return driver
    }
}
