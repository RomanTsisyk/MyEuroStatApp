package eu.eurostat.core.database

import app.cash.sqldelight.db.SqlDriver

actual class DatabaseDriverFactory {
    actual fun create(): SqlDriver = error("DatabaseDriverFactory is not supported on JVM. Use JdbcSqliteDriver directly in tests.")
}
