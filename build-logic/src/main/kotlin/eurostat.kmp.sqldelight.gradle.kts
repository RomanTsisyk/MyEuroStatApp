plugins {
    id("eurostat.kmp.library")
    id("app.cash.sqldelight")
}

val libs = the<org.gradle.accessors.dm.LibrariesForLibs>()

sqldelight {
    databases {
        create("AppDatabase") {
            packageName.set("eu.eurostat.core.database.generated")
        }
    }
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines.ext)
            implementation(libs.kotlinx.datetime)
        }
        androidMain.dependencies { implementation(libs.sqldelight.android.driver) }
        iosMain.dependencies { implementation(libs.sqldelight.native.driver) }
        val desktopMain by getting {
            dependencies { implementation(libs.sqldelight.sqlite.driver) }
        }
    }
}
