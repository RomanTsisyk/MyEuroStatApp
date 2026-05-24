package eu.eurostat.core.database

import eu.eurostat.core.database.generated.AppDatabase

fun createAppDatabase(driverFactory: DatabaseDriverFactory): AppDatabase =
    AppDatabase(driver = driverFactory.create())
