plugins { id("eurostat.kmp.sqldelight") }

android.namespace = "eu.eurostat.core.database"

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.coreCommon)
            implementation(libs.koin.core)
        }
        val desktopTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.sqldelight.sqlite.driver)
                implementation(libs.kotlinx.coroutines.test)
            }
        }
    }
}

// core-common has no JVM/desktop variant — exclude it from desktop configurations so
// desktopTest compilation and runtime work without it.
configurations.matching { it.name.startsWith("desktop") }.configureEach {
    exclude(module = "core-common")
}

