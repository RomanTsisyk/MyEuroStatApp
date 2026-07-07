plugins { id("eurostat.kmp.library") }

android.namespace = "eu.eurostat.core.common"

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.koin.core)
            // api: KSerializer/Json and Instant appear in the public cache API
            // (JsonBlobCache, CachedValue) consumed by feature modules.
            api(libs.kotlinx.serialization.json)
            api(libs.kotlinx.datetime)
        }
    }
}
