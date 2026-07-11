import java.util.Properties
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    id("eurostat.android.application")
}

android {
    namespace = "eu.eurostat.app"
    defaultConfig {
        applicationId = "eu.eurostat.app"
        versionCode = 60
        versionName = "0.6.0"
        resourceConfigurations += listOf("en", "pl", "uk")
    }

    // Release signing: read from keystore.properties when present, fall back
    // to the debug keystore so contributors and the F-Droid build server can
    // assemble installable APKs without manual ceremony. The real signing
    // identity lives outside the repo (see docs/RELEASING.md).
    val keystorePropsFile = rootProject.file("keystore.properties")
    val releaseSigning = if (keystorePropsFile.exists()) {
        val props = Properties().apply { keystorePropsFile.inputStream().use { load(it) } }
        signingConfigs.create("release") {
            storeFile = rootProject.file(props.getProperty("storeFile"))
            storePassword = props.getProperty("storePassword")
            keyAlias = props.getProperty("keyAlias")
            keyPassword = props.getProperty("keyPassword")
        }
    } else null

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = releaseSigning ?: signingConfigs.getByName("debug")
        }
    }
}

kotlin {
    sourceSets {
        // Compose runtime goes in commonMain so the wasmJs stub composable can use it.
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
        }

        // "nativeAppMain" is an intermediate source set shared by Android + desktop + iOS.
        // It intentionally excludes wasmJs so that SQLDelight-dependent modules (core-database,
        // feature-*) are not resolved for the Wasm target. See CLAUDE.md § Phase 5 for the
        // full SQLDelight-wasmJs limitation note.
        val nativeAppMain by creating {
            dependsOn(commonMain.get())
            dependencies {
                implementation(projects.coreCommon)
                implementation(projects.coreJsonstat)
                implementation(projects.coreNetwork)
                implementation(projects.coreDatabase)
                implementation(projects.coreNavigation)
                implementation(projects.coreUi)
                implementation(projects.featurePopulation)
                implementation(projects.featureEconomy)
                implementation(projects.featureEnvironment)
                implementation(projects.featureTrade)
                implementation(projects.featureTransport)
                implementation(projects.featureTourism)
                implementation(projects.featureSocial)
                implementation(projects.featureScience)
                implementation(projects.featureSettings)
                implementation(projects.featureOverview)
                implementation(projects.featureSearch)
                implementation(projects.featureCompare)
                implementation(libs.koin.core)
                implementation(libs.decompose.core)
                implementation(libs.decompose.extensions.compose)
                implementation(libs.essenty.lifecycle)
            }
        }
        androidMain.get().dependsOn(nativeAppMain)
        iosMain.get().dependsOn(nativeAppMain)
        val desktopMain by getting {
            dependsOn(nativeAppMain)
            dependencies {
                implementation(compose.desktop.currentOs)
                // Required for Dispatchers.Main on JVM/desktop (Swing event loop).
                implementation(libs.kotlinx.coroutines.swing)
            }
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.lifecycle.runtime.ktx)
            implementation(libs.koin.android)
        }
        val wasmJsMain by getting {
            // wasmJs intentionally has NO feature/database dependencies.
            // SQLDelight 2.0.x has no wasmJs variant — see TODO in CLAUDE.md.
        }
    }
}

compose.desktop {
    application {
        mainClass = "eu.eurostat.app.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "EU Stats"
            // Tracks the app version at release time.
            packageVersion = "1.0.0"
            description = "Independent open-source client for the public Eurostat API"
            vendor = "Roman Tsisyk"
            licenseFile.set(rootProject.file("LICENSE"))

            linux {
                iconFile.set(project.file("icons/app.png"))
            }
            macOS {
                iconFile.set(project.file("icons/app.icns"))
                bundleID = "eu.eurostat.app"
            }
            windows {
                iconFile.set(project.file("icons/app.ico"))
                menuGroup = "EU Stats"
            }
        }
    }
}
