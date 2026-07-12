plugins { id("eurostat.kmp.compose") }

android.namespace = "eu.eurostat.feature.overview"

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.coreCommon)
            implementation(projects.coreUi)
            implementation(projects.coreNavigation)
            // Header pill icons beyond the material-icons core set (SsidChart).
            implementation(compose.materialIconsExtended)

            // The Overview aggregates one teaser metric per feature, so it depends
            // on every feature's public domain layer (repository interface + models).
            implementation(projects.featurePopulation)
            implementation(projects.featureEconomy)
            implementation(projects.featureEnvironment)
            implementation(projects.featureTrade)
            implementation(projects.featureTransport)
            implementation(projects.featureTourism)
            implementation(projects.featureSocial)
            implementation(projects.featureScience)

            implementation(libs.decompose.core)
            implementation(libs.essenty.lifecycle)
            implementation(libs.essenty.lifecycle.coroutines)
            implementation(libs.koin.core)
        }
    }
}
