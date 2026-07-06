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
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
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
