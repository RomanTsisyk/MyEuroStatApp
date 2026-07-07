pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
dependencyResolutionManagement {
    // PREFER_SETTINGS (not FAIL_ON_PROJECT_REPOS): the Kotlin/JS + wasmJs Node.js
    // plugin adds the "https://nodejs.org/dist" distributions repository at the
    // project level when configuring wasmJs test tasks. FAIL_ON_PROJECT_REPOS
    // rejects that ("added by unknown code"), breaking `./gradlew allTests` in CI.
    // PREFER_SETTINGS keeps our settings repositories authoritative for normal
    // dependency resolution while allowing plugin-injected toolchain repos.
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        // Node.js distributions for the Kotlin/JS + wasmJs test toolchain. Declared
        // here (not left to the plugin's project-level injection) so it resolves
        // under PREFER_SETTINGS; scoped to org.nodejs:node so it can't shadow other
        // dependencies. Fixes `:kotlinNodeJsSetup` failing `./gradlew allTests` in CI.
        ivy("https://nodejs.org/dist/") {
            name = "Node Distributions"
            patternLayout { artifact("v[revision]/[artifact](-v[revision]-[classifier]).[ext]") }
            metadataSources { artifact() }
            content { includeModule("org.nodejs", "node") }
        }
        // Yarn distributions — same story as Node.js above (Kotlin JS/wasmJs toolchain).
        ivy("https://github.com/yarnpkg/yarn/releases/download/") {
            name = "Yarn Distributions"
            patternLayout { artifact("v[revision]/[artifact](-v[revision]).[ext]") }
            metadataSources { artifact() }
            content { includeModule("com.yarnpkg", "yarn") }
        }
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "MyEuroStatApp"

include(":composeApp")

include(":core-common")
include(":core-jsonstat")
include(":core-network")
include(":core-database")
include(":core-navigation")
include(":core-charts")
include(":core-ui")

include(":feature-population")
include(":feature-economy")
include(":feature-environment")
include(":feature-trade")
include(":feature-transport")
include(":feature-tourism")
include(":feature-social")
include(":feature-science")
include(":feature-settings")
include(":feature-overview")
include(":feature-search")
