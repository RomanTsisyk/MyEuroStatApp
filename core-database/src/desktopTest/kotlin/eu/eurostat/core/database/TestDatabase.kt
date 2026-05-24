package eu.eurostat.core.database

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import eu.eurostat.core.database.generated.AppDatabase

fun createInMemoryDatabase(): AppDatabase {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    AppDatabase.Schema.create(driver)
    return AppDatabase(driver = driver)
}
