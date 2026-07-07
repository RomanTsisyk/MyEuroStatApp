# Changelog

All notable changes to this project are documented here.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/);
this project follows [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased] — v0.6 development (branch `develop-v0.6`)

### Added

- Search screen (`feature-search`): a compiled-in index of 27 indicators
  across the 8 modules (labels, dataset codes, natural-language keywords)
  with pure tiered ranking (label prefix > word prefix > substring >
  keyword > description) and browse-by-module on a blank query; reachable
  from a search pill on the Overview header, and opening a result brings
  the target module to front
- The persisted default-country preference now seeds every feature
  component's (and the Overview's) first query — read once before the
  first fetch, no double-fetch, no wrong-country flash; mid-session
  changes apply on next start
- Tablet/desktop responsive master-detail via a shared three-slot
  `AdaptiveTwoPane` in core-ui: exact phone ordering below 840 dp, a
  320 dp controls pane beside a content pane at ≥ 840 dp; adopted by all
  8 feature screens; desktop window opens at 1280×800 (visually verified
  at 400/1280 dp on the desktop target)
- KMP-level PL/UK localization, started: per-module Compose Resources
  `strings.xml` (EN/PL/UK) for the shared core-ui components and the
  population + science screens, resolving via `stringResource(...)`
  (pattern proven; the other 6 screens + applying the language
  preference are next)
- CI: a compile-only iOS gate on `macos-latest`
  (`:composeApp:compileKotlinIosSimulatorArm64`, gated behind the Linux
  job) so iOS-only breakage is caught without spending simulator minutes

### Fixed

- Search crashed on Kotlin/Native (iOS): a top-level
  `Regex("[^\\p{L}\\p{N}&]+")` threw at construction (Native rejects the
  `\p{L}`/`\p{N}` Unicode-property classes) and took the whole file's
  initializer down, failing every ranking call on iOS while passing on
  the JVM — replaced with a portable character-scan word splitter
- Intermittent test failures: the 8 API-service test suites captured
  parallel MockEngine requests into an unsynchronized list; now guarded
  by a `Mutex`-backed recorder

### Planned for v0.6 / v1.0

- Finish PL/UK localization: the remaining 6 feature screens + apply the
  stored language preference at runtime so the Settings picker switches
  UI language
- Bundled SVG flags via Compose Resources; dedicated cross-module compare
  screen (data-layer scaffold recoverable from git history)
- Verified launch on a physical iPhone (TestFlight); on-device Android
  smoke run; interactive 8-tab walk-through → `RUN_REPORT.md`
- Native Windows / macOS installers (`.msi`, `.dmg`) from Compose Desktop
- F-Droid inclusion (metadata.yml prepared in `metadata/` — pending fdroiddata MR)
- GitHub Actions release workflow with reproducible signed APK

## [0.5.0] — 2026-07-07

### Added

- Convergent JSON-blob cache (`MultiDimCacheEntity` + `JsonBlobCache<T>`):
  population age-cohort frames now cached (pyramid works offline) and
  tourism switched off the residence-tall table with its `-1` sentinel;
  SQLDelight migrations infrastructure (`1.sqm`, `2.sqm`, schema v3) so
  existing installs upgrade in place
- Settings persistence: theme (system/light/dark, applied app-wide),
  language and default-country preferences stored in SQLDelight;
  functional Clear-cache; settings gear on the Overview header
- Comparison mode on the economy chart: index-stable `SeriesPalette`
  colors for any number of countries + "Absolute / Indexed 100"
  normalization toggle (each series rebased to 100 at the first visible
  year)
- Shared locale-aware number formatting in `core-ui`
  (`eu.eurostat.ui.format`): platform decimal/grouping separators via
  expect/actual, one rounding convention, magnitude buckets with
  rollover promotion; all eight screens migrated off inline formatters
- Pull-to-refresh on all eight feature screens (Material3
  `PullToRefreshBox`)
- One shared `AppError.toUserMessage()` mapping with clearer human copy
- Chart recomposition wins: heavy pure derivations hoisted into
  `remember(inputs)` across six chart types

### Fixed

- Desktop database driver crashed on the second launch (unconditional
  `Schema.create()`); now schema-managed with migrations
- iOS app now actually builds and launches in the simulator: the stub
  `project.pbxproj` was replaced with a real XcodeGen-generated project
  (`iosApp/project.yml`), plus `-lsqlite3` linking, the
  `CADisableMinimumFrameDurationOnPhone` Info.plist key, the
  `doInitKoinIos()` export-name fix and JAVA_HOME pinning in the gradle
  pre-build phase; toolchain builds work with
  `DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer` when
  `xcode-select` points at CommandLineTools (permanent fix:
  `sudo xcode-select -s`)

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

[Unreleased]: https://github.com/RomanTsisyk/MyEuroStatApp/compare/v0.5.0...HEAD
[0.5.0]: https://github.com/RomanTsisyk/MyEuroStatApp/compare/v0.4.0...v0.5.0
[0.4.0]: https://github.com/RomanTsisyk/MyEuroStatApp/releases/tag/v0.4.0
