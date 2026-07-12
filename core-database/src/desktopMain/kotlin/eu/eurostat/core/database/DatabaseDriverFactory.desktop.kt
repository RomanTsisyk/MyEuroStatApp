package eu.eurostat.core.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import eu.eurostat.core.database.generated.AppDatabase
import java.util.Properties

actual class DatabaseDriverFactory {
    actual fun create(): SqlDriver =
        // Passing the schema lets the driver manage sqlite's user_version:
        // fresh files get Schema.create(), existing files get migrations
        // (e.g. 1.sqm adding MultiDimCacheEntity). The previous unconditional
        // Schema.create() call crashed on the second launch ("table already
        // exists") and could never migrate.
        JdbcSqliteDriver(
            url = "jdbc:sqlite:eurostat.db",
            properties = Properties(),
            schema = AppDatabase.Schema,
        )
}
