# Changelog

All notable changes to this project are documented here.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/);
this project follows [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Planned for v0.5 / v1.0

- Overview dashboard screen wired to `BottomTabDestination.Overview`
- Searchable country picker (list + map variants) and multi-country comparison overlay
- Search & Settings screens
- Tablet master-detail layouts at the `Expanded` breakpoint
- Real flag rendering and locale-aware number formatting
- iOS Xcode wrapper regeneration; verified launch on iPhone + iPad simulator
- Native Windows / macOS installers (`.msi`, `.dmg`) from Compose Desktop
- F-Droid inclusion (metadata.yml prepared in `metadata/` — pending fdroiddata MR)
- GitHub Actions release workflow with reproducible signed APK

## [0.4.0] — 2026-05-29

First public release. The codebase has been hardened end-to-end and is
the snapshot referenced in the NLnet NGI Zero Commons Fund proposal.

### Added

- All eight feature modules ship real Eurostat public-API data with no
  hard-coded mocks: `feature-population` (`demo_pjangroup`),
  `feature-economy` (`nama_10_gdp` + `prc_hicp_aind` + `gov_10dd_edpt1`),
  `feature-environment` (`env_air_gge` + `nrg_bal_c` + `sdg_13_10`),
  `feature-trade` (`ext_lt_intratrd`), `feature-transport`
  (`road_pa_buscoa` + `avia_paoc`), `feature-tourism`
  (`tour_occ_ninat` + `tour_occ_nim`), `feature-social`
  (`ilc_li02` + `ilc_peps01` + `hlth_silc_01`) and `feature-science`
  (`rd_e_gerdtot` + `isoc_ci_ifp_iu` + `edat_lfse_03`)
- Adaptive launcher icon (`mipmap-anydpi-v26` + legacy mipmaps at
  mdpi/hdpi/xhdpi/xxhdpi/xxxhdpi) — EU-blue background with a white
  bar-chart foreground and yellow EU-star accent
- English, Polish and Ukrainian app strings (`values/`, `values-pl/`,
  `values-uk/`); `resourceConfigurations` pinned to `en, pl, uk`
- `fastlane/metadata/android/{en-US,pl,uk}/` listing metadata: title,
  short and full descriptions, changelog, 512×512 icon,
  1024×500 feature graphic, nine phone screenshots
- `metadata/eu.eurostat.app.yml` — F-Droid build recipe ready for the
  fdroiddata merge request
- `docs/FDROID.md` — user-facing install guide
- Release signing wired in `composeApp/build.gradle.kts`: reads
  `keystore.properties` when present and falls back to the debug
  keystore so contributors and the F-Droid build server can produce
  installable APKs without manual ceremony

### Changed

- Unified `applicationId` and `namespace` to `eu.eurostat.app`
  (previously diverged as `eu.eustats.app` / `eu.eurostat.app`)
- Bumped `versionCode` to `40` and `versionName` to `0.4.0` to match
  the phase-4 state referenced in `README.md` and the NLnet proposal

### Known limitations

- iOS Xcode wrapper project (`iosApp/iosApp.xcodeproj`) is a placeholder
  stub and needs to be regenerated locally before iOS builds work; the
  KMP common code compiles for all three iOS targets (`iosArm64`,
  `iosX64`, `iosSimulatorArm64`)
- The Overview bottom-nav destination is wired but the screen is empty
- No tablet master-detail layouts yet — phone UI scales but does not
  reflow to two columns at the `Expanded` breakpoint

## [0.3.0] — 2026-05-17 *(internal milestone)*

### Added

- Phase 3 hardening: `feature-environment` end-to-end with three
  parallel datasets, sector breakdown and a cache layer; `commonTest`
  coverage restored after the data-layer refactor

## [0.2.0] — 2026-05-16 *(internal milestone)*

### Added

- Phase 2: `feature-population` end-to-end with 18 demographic cohorts
  (Y_LT5 → Y_GE85) and a pyramid hero chart driven by `demo_pjangroup`

## [0.1.0] — 2026-05-16 *(internal milestone)*

### Added

- Phase 0 / Phase 1 foundation: `core-common` (`Result<T>`, `AppError`,
  `DispatcherProvider`), `core-jsonstat` (JSON-stat 2.0 parser),
  `core-network` (shared Ktor `HttpClient` + `EurostatApiClient`),
  `core-database` (SQLDelight schema + DAO scaffolding),
  `core-navigation` (Decompose `RootComponent` + `ChildConfig`),
  `core-ui` (full design system: tokens + 20 components + 5 state
  composables + `EurostatTheme` with the `object Euro` accessor) and
  `core-charts` (8 chart types on a pure Compose `Canvas`,
  Koalaplot removed)
- Gradle convention plugins (`eurostat.kmp.library`,
  `eurostat.kmp.compose`, `eurostat.kmp.sqldelight`,
  `eurostat.android.application`) under `build-logic/`
- Centralised version catalog (`gradle/libs.versions.toml`)

[Unreleased]: https://github.com/RomanTsisyk/MyEuroStatApp/compare/v0.4.0...HEAD
[0.4.0]: https://github.com/RomanTsisyk/MyEuroStatApp/releases/tag/v0.4.0
