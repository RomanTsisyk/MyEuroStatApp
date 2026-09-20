# Changelog

All notable changes to this project are documented here.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/);
this project follows [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased] — v0.7 development (branches `develop-v0.7`, `develop-v0.8`)

### Added

- Polish and Ukrainian country names: `countryDisplayName(code, fallback)`
  backed by 34 `country_*` strings (EN/PL/UK) replaces the English-only
  names in the screen headers, radar legend, headlines and country picker
  (picker search also matches the localized name)
- Real SVG-derived flags: 33 circle-flags (HatScripts, MIT) bundled as
  Compose vector drawables in core-ui; new `CountryFlag` composable
  (circle-cropped `Image`, emoji fallback for flagless aggregates like
  EA20) now renders in `CountryChip`, `CountryPickerSheet` and the
  Settings default-country row — replaces the Unicode-emoji flags
- Detail modal on chart tap: tapping near a point on the line chart opens
  a bottom sheet with country, year, formatted value, unit and dataset
  code (localized EN/PL/UK); pure `nearestChartPoint` hit-test in
  core-charts (unit-tested) shares the exact draw-pass coordinate
  mapping, wired on the Economy and Environment hero charts and the
  Compare screen
- The header search icon now opens Search from Population, Economy,
  Environment, Social, Science and Compare (it was a dead button); Back
  returns to the module it was opened from. Trade, Transport and Tourism
  show no search icon. The app bar's Back / Search / Refresh / More
  accessibility descriptions are localized (EN/PL/UK)
- Store changelogs for `versionCode` 60 (`fastlane/metadata/android/{en-US,pl,uk}/changelogs/60.txt`)
- Tag-triggered release workflow (`.github/workflows/release.yml`):
  pushing a `v*` tag builds the release APK (real signature when the
  keystore secrets are configured, debug-signed fallback otherwise) and
  the three native desktop installers on a macOS/Windows/Ubuntu matrix,
  attaching everything to the GitHub Release for the tag

- feature-compare: a dedicated cross-module comparison screen — pick one
  of 8 headline indicators (population, GDP, GHG, exports, air
  passengers, tourism nights, at-risk-of-poverty, R&D %GDP) and 2-5
  countries; one palette-colored line per country overlaid on a single
  chart with an Absolute / Indexed-100 toggle. `rebaseToIndex` is now
  public in `core-charts` (feature-economy's internal duplicate deleted);
  reachable via a compare pill on the Overview header; EN/PL/UK
  localized; unit-tested including `iosSimulatorArm64Test`
- KMP-level PL/UK localization now covers **all** modules: Compose
  Resources `strings.xml` (EN/PL/UK) extracted for the remaining 9
  modules (economy, environment, trade, transport, tourism, social,
  settings, search, overview), completing the pattern started on core-ui
  + population + science
- Runtime language switching: `LocalAppLocale` expect/actual
  (Android/desktop/iOS) following the official Compose
  resource-environment pattern; `EurostatApp` observes
  `AppPreferences.language` and rebuilds the themed subtree on change —
  the Settings language picker now actually switches the UI language
  immediately, not just on next launch
- AppError messages now localize: UI states carry the raw `AppError`;
  a new `@Composable AppError.localizedMessage()` in core-ui (EN/PL/UK)
  resolves it at the `ErrorState` call site so error copy follows
  runtime language switches; the old English-only
  `core-common/AppError.toUserMessage()` is deleted
- iOS app icon: a single-size 1024×1024 no-alpha `AppIcon.appiconset`
  (upscaled from the 512 px fastlane store icon), wired through
  `project.yml` and regenerated via XcodeGen
- Desktop native installers: `nativeDistributions` (`Dmg`/`Msi`/`Deb`)
  with per-OS icons (`.icns`/`.ico`/`.png` generated from the store
  icon), package name "EU Stats"; `packageDmg` verified locally
  (`EU Stats-1.0.0.dmg`, 122 MB)
- CI: push trigger now includes `develop-v*`; the iOS job runs the real
  `iosSimulatorArm64Test` suite instead of a compile-only gate; the
  Android job also runs `assembleRelease` (exercises R8/proguard,
  falls back to the debug keystore); a new Ubuntu `desktop` job smoke-tests
  `packageUberJarForCurrentOS`
- Version sync: `versionCode` 60 / `versionName` 0.6.0 across Android,
  the Settings About screen (no longer hardcoded to 0.4.0), and the iOS
  `Info.plist`

### Changed

- Documentation media regenerated from the fixed build: demo GIFs
  (`docs/gifs`) re-recorded, landing-page stills (`docs/assets/screens`) and
  the Android store screenshots re-shot

### Fixed

- CI `ios-test`: the Kotlin/Native compiler ran out of heap (`OutOfMemoryError`)
  building its cache for `material-icons-extended`, an ~11 000-icon library of
  which the app used nine icons. Those nine are now bundled as plain vectors
  (`EuroIcons` in `core-ui`, path data from Material Icons, Apache-2.0) and the
  dependency is gone, leaving `material-icons-core` for the rest. Locally, the
  Native link and the whole iOS test suite now run in 45 s with the default heap;
  the CI run of this branch is the confirmation
- iOS: the header of every module screen (Back, Search, Refresh) was drawn under
  the system status bar, so Back could not be tapped and a user could not leave a
  module; `IosModuleAppBar` now applies the status-bar inset like the Android bar
- Offline errors: a device that is really offline throws plain `java.net`
  exceptions (`UnknownHostException`, `SocketException`, …) which were not
  recognised as no-connectivity, so the error state said "Something went
  wrong" instead of "No connection — check your network". Fixed for
  Android and desktop (`java.net` exceptions) and iOS (`NSURLErrorDomain`
  codes, classified from the Darwin error text; the classifier is unit-tested
  on the JVM, the iOS glue is compiled only by the CI iOS job)
- Status bar: icons now follow the theme chosen in Settings (dark icons on
  a dark background made the clock invisible on every screen), and the
  Overview's dark header gets light icons
- Overview kept the previous locale's number format (`4,387`, `83.5M`)
  after a language switch until restart; teasers now hold raw values and
  are formatted while composing
- Transport: the air-passengers series was always empty. `avia_paoc` was
  queried with `schedule=TOT`, but the Eurostat code is `TOTAL`; the API
  silently accepts unknown codes and returns zero rows, so the AIR tile
  and AIR chart never showed data (the Overview teaser quietly fell back
  to road passengers). Regression tests assert
  the `schedule=TOTAL` filter; the default year now follows the latest
  road year so the headline is not "—" when air data runs one year further
- Line-chart year axis rendered `2K … 2K` instead of `2010 … 2024` on
  Economy, Environment and Compare — the default compact K/M/B axis
  formatter was applied to years; new `yearAxis()` in `core-charts`
- Trade: the Exports / Imports / Balance tabs collapsed (first tab took
  the whole row, the others wrapped one letter per line and left a blank
  gap) — `UnderlineTabs` now sizes each tab to its label
- Trade: the headline number always showed the balance while the subtitle
  named the selected tab; it now follows Exports / Imports / Balance
- Science: the three sparkline tiles always ended at the latest year, so
  the value under an earlier selected year disagreed with the headline; they
  now read "as of the selected year"
- Number formatting consistency: Economy and Trade truncated whole billions
  (4,386 / 839) while Overview rounded (4,387 / 840) — all three now round;
  Trade uses the typographic minus (`−14`); Environment energy/GHG values
  use locale grouping (`177,745` instead of `177745`)
- KPI tiles no longer break units mid-word (`G/DP`, `ilc_li/02`) on Social
  and Science and now have equal heights; Transport stat values stay on one
  line (`200 M`); the Science radar draws its axis labels and names the
  compared regions ("Germany", "EU (27)") instead of raw codes
- Search crashed on Kotlin/Native (iOS): a top-level
  `Regex("[^\\p{L}\\p{N}&]+")` threw at construction (Native rejects the
  `\p{L}`/`\p{N}` Unicode-property classes) and took the whole file's
  initializer down, failing every ranking call on iOS while passing on
  the JVM — replaced with a portable character-scan word splitter
- Intermittent test failures: the 8 API-service test suites captured
  parallel MockEngine requests into an unsynchronized list; now guarded
  by a `Mutex`-backed recorder

### Planned for v0.8 / v1.0

- Verified launch on a physical iPhone (TestFlight) and the iOS simulator
  walk-through; a run on a physical Android device (the emulator
  walk-through is done — see `RUN_REPORT.md`)
- Real Android release signing key (currently debug-keystore fallback);
  Msi/Deb installers verified via CI (only Dmg verified locally so far)
- F-Droid inclusion (metadata.yml prepared in `metadata/` — pending fdroiddata
  MR; the recipe still pins `v0.4.0` / `versionCode` 40 and must be updated
  to the tag cut for the next release)

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
