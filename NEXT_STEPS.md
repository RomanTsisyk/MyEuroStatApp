# Next Steps — Path to 100% Working App

**Goal:** an Android + iOS app that launches, navigates, fetches real Eurostat data, renders every screen without crashes, recovers from offline/error, and ships.

Tasks ranked by what blocks "100% working" hardest. Each task is self-contained: prompt + files + acceptance.

Legend: **P0** ship-blocker · **P1** breaks UX · **P2** quality/consistency · **P3** polish.

---

## P0 · Smoke-test the app on a real Android device

**Why:** the app builds (APK 19 MB) but has never been run end-to-end on a real device. Any of: API call failure, JSON-stat parse error on real responses, Compose crash on first render, can break "100% working" instantly.

**Steps:**
1. `./gradlew :composeApp:installDebug` on connected device or emulator.
2. Open each of the 8 tabs in order. For each: confirm headline shows real number (not "0" or "—"), chart renders, no Compose crash, refresh button works.
3. Toggle airplane mode → verify offline state (`OfflineBanner` + cached data OR `ErrorState`).
4. Re-enable network → tap refresh → verify fresh data lands.
5. Rotate device → verify Compose state (selected country, year range, switcher selection) survives.
6. Record findings as `RUN_REPORT.md` with screenshots.

**Files involved:** none to edit. Pure verification.

**Acceptance:** all 8 tabs render real data without crash on at least one Android device. Any crashes become P0 bug tickets.

---

## P0 · Fix iOS toolchain so iosApp can build

**Why:** `xcrun xcodebuild -version` returns exit 72 — Xcode CLI not properly configured. Every agent's `:build` failed at `linkDebugTestIosX64`. iOS half of "Kotlin Multiplatform" is unverified.

**Steps:**
1. Run `xcode-select -p` — confirm path points to a real Xcode.app, not just Command Line Tools.
2. If pointing to CLT: `sudo xcode-select -s /Applications/Xcode.app/Contents/Developer`.
3. Run `xcrun xcodebuild -version` — should print version, not error.
4. Try `./gradlew :composeApp:linkDebugTestIosSimulatorArm64` → must succeed.
5. Open `iosApp/iosApp.xcodeproj`, set a simulator target, ⌘R → verify app launches.
6. Run each tab same as P0 smoke test above.

**Files involved:** possibly `iosApp/iosApp.xcodeproj` signing config; `iosApp/Configuration/Config.xcconfig` (deployment target).

**Acceptance:** `./gradlew :composeApp:build` passes ALL targets (no iOS link failures). App launches in iOS simulator and renders all 8 tabs.

---

## P0 · Add an Overview / landing dashboard screen

**Why:** the app currently opens on `HomeScreen` — a responsive 8-card module grid (`composeApp/src/nativeAppMain/.../home/HomeScreen.kt`), launched via `ChildConfig.Home` (the initial config in `RootComponent`). There is **no** `Overview` destination, `feature-overview` module, or `BottomTabDestination.Overview` member — the design brief's Overview (hero stat + per-module teaser metrics) is not built yet. The HomeScreen grid is a functional landing but not the data-rich dashboard the brief describes.

**Steps:**
1. Decide: enrich `HomeScreen` into the dashboard, OR add a dedicated `ChildConfig.Overview` + `feature-overview` aggregator module (copy `feature-population/` without a data layer). Pick the cleaner of the two.
2. Implement the dashboard matching `design/screens-overview.jsx` `ScreenOverviewA` (hero metric + 2-column module tile grid). For each tile, show a teaser metric from the respective feature repository, or a static label + icon if cross-module wiring is too coupled.
3. If a new destination: add it to `ChildConfig.kt`, the `RootComponentFactory`, and `composeApp/.../EurostatApp.kt`'s `Children` dispatcher; set it (or keep `Home`) as the initial config.

**Files involved:** `composeApp/src/nativeAppMain/.../home/HomeScreen.kt`, `core-navigation/.../ChildConfig.kt`, `core-navigation/.../RootComponent.kt`, `composeApp/.../EurostatApp.kt`, optional new `feature-overview/` module + `settings.gradle.kts`.

**Acceptance:** the landing screen shows a hero stat + per-module teaser metrics, not just a static grid.

---

## ✅ DONE (v0.4.0) · Environment commonTest restored

**Status:** ✅ Done. `feature-environment/src/commonTest/` has 4 test classes — `EnvironmentApiServiceImplTest`, `EnvironmentCellMapperTest`, `EnvironmentRepositoryImplTest`, `EnvironmentComponentTest` — plus fakes, all green in the 444-test suite. Original task notes kept below for reference.

**Steps:**
1. List the 8 deleted files from `git show HEAD -- feature-environment/src/commonTest/` (they were in `initial` commit before the refactor; if not committed, recreate from the patterns in `feature-population/src/commonTest/` and `feature-tourism/src/commonTest/`).
2. Write tests for:
   - `EnvironmentCellMapperTest` — GHG cells per sector → correct EnvSector enum mapping; SDG cells → sector-null points; merger combines all 3 datasets without losing data.
   - `EnvironmentApiServiceImplTest` — uses FakeEurostatApiClient, asserts 3 parallel fetches with correct filter maps (cross-check against `CLAUDE.md` env_air_gge / nrg_bal_c / sdg_13_10 rows).
   - `EnvironmentRepositoryImplTest` — emits Loading → fresh Success; on network failure emits Error (no cache yet since cache bypassed).
   - `EnvironmentComponentTest` — Turbine: initial Loading; Refresh intent triggers re-fetch.
3. Don't restore cache tests — cache is intentionally bypassed (`EnvironmentRepositoryImpl` KDoc explains).

**Files involved:** `feature-environment/src/commonTest/kotlin/eu/eurostat/feature/environment/*` (new).

**Acceptance:** `./gradlew :feature-environment:allTests` passes with at least 4 test classes covering the 4 layers.

---

## ✅ DONE (v0.4.0) · Tourism seasonality heatmap — real `tour_occ_nim`

**Status:** ✅ Done. `TourismApiServiceImpl.fetchSeasonality()` fetches real `tour_occ_nim` monthly nights (`c_resid=TOTAL`, `unit=NR`, `nace_r2=I551/I552/I553`); the heatmap renders them. `placeholderHeatmapCells()` is a loading-only fallback — there is no `mockHeatmapCells()`. Original notes kept below for reference.

**Steps:**
1. Add `tour_occ_nim` row to the dataset table in `CLAUDE.md` (filter: `c_resid=DOM/FOR/TOTAL`, `unit=NR`, `nace_r2=I551-I553`, `time=monthly` format `YYYY-MM`).
2. Extend `TourismApiServiceImpl` with a 3rd parallel `async` for monthly nights.
3. Extend `TourismDataPoint` or add `TourismMonthlyPoint(year, month, nights)`. Likely cleaner: separate `TourismSeasonality(countryCode, cells: List<List<Long>>)` keyed by (year, month).
4. Pipe through `TourismUiState.Content.seasonality: TourismSeasonality?`.
5. In `TourismScreen`, replace `mockHeatmapCells()` with `state.seasonality?.cells?.map { row -> row.map { it.toFloat() } }`.

**Files involved:** `feature-tourism/src/commonMain/.../data/TourismApiServiceImpl.kt`, `data/TourismCellMapper.kt`, `domain/TourismModel.kt`, `ui/TourismUiState.kt`, `ui/TourismComponent.kt`, `ui/TourismScreen.kt`.

**Acceptance:** `./gradlew :feature-tourism:compileDebugKotlinAndroid` passes. Heatmap shows real seasonality (peaks visible in summer months for southern EU countries).

---

## P1 · Cache strategy convergence

**Why:** modules inconsistently handle cache for multi-dimensional data:
- Environment: full bypass (no DAO).
- Population: caches totals only, bypasses age cohorts.
- Tourism: writes 3 entity rows per point against the existing residence-tall schema.
- Economy/Trade/Transport/Social/Science: standard SQLDelight cache through DAO.

This is a maintenance landmine. Pick one convergent approach.

**Recommended approach:** add a generic `core-database` table `MultiDimCache(key: String, dataJson: String, fetchedAt: Long)`. Each repository serializes its full domain shape to JSON via `kotlinx.serialization`, keyed by `query.hashCode().toString()`. 12h TTL stale check unchanged.

**Steps:**
1. Add `MultiDimCache.sq` schema + DAO interface in `core-database`.
2. Add a `JsonBlobCache<T>` helper in `core-common` with `get(key) → T?`, `put(key, T, fetchedAt)`, `isStale(key, ttlMs)`.
3. Migrate Environment first: drop bypass, add `JsonBlobCache<List<EnvironmentTimeSeries>>` in repository.
4. Migrate Population (add cohort cache alongside existing total cache).
5. Migrate Tourism (collapse 3-row split into single JSON blob).
6. Verify all tests still pass.

**Files involved:** new `core-database/src/commonMain/.../MultiDimCache.sq` + DAO; new `core-common/.../JsonBlobCache.kt`; 3 feature repositories.

**Acceptance:** all 8 feature repositories use the same caching pattern. Environment cache no longer bypassed.

---

## P1 · Remove dead code

**Why:** keeps the legacy `eu.eurostat.core.ui.*` package and `WireframeApp.kt` alive in `composeApp`. Confusing for new contributors; potential drift target.

**Steps:**
1. `rg "eu.eurostat.core.ui" --type kotlin` — list every file that still imports the legacy theme.
2. Migrate each callsite to `eu.eurostat.ui.*`. Most likely the only remaining users are `FeatureScaffold`, `StaleIndicator`, `StatusViews`, `Placeholder`, `WireframeApp` (kill it).
3. Delete `composeApp/src/commonMain/kotlin/eu/eurostat/app/WireframeApp.kt`.
4. Delete `core-ui/src/commonMain/kotlin/eu/eurostat/core/ui/` and all `sketch/` primitives (`SketchBox`, `SketchPalette`, `SketchTabBar`, `SketchPyramid`, `SketchChoropleth`, `WireframeGallery`, `DeviceFrames`, `SketchPrimitives`, `SketchIcons`, `SketchSpacing`, `SketchTypography`, `SketchPaper`, `SketchCharts`).
5. Confirm no orphaned imports: `rg "Sketch|Wireframe" --type kotlin`.

**Files involved:** ~15 deletions, ~5-8 import migrations.

**Acceptance:** `./gradlew :composeApp:assembleDebug` still passes. `rg "Sketch|Wireframe|core.ui.theme" --type kotlin` returns no production refs.

---

## P1 · Country picker screen + real flag rendering

**Why:** `CountryChipsRow` has an "+ add" CTA that does nothing. There's no picker. Flag swatches are 14×10 dp placeholder rects.

**Steps:**
1. Implement `feature-picker/` (or put it in `core-ui/component/picker/` since it's UI-only). Two variants per `design/screens-picker.jsx`: searchable list + tap-the-map.
2. Wire `+ add` chip → opens picker as `ModalBottomSheet` or full-screen overlay.
3. Real flag rendering: 27 EU + 4 EFTA SVG flags. Options:
   - Bundle Twemoji country flag emoji as a font and render `Text("🇩🇪")` — simplest, KMP-friendly.
   - Bundle individual SVGs in `core-ui/resources/flags/` and render via `painterResource(Res.drawable.flag_de)` — requires Compose Resources setup.
4. Update `CountryChip.kt` to take a `code: String` and resolve the flag.
5. Map screen uses the existing `EurostatChoropleth` from core-charts.

**Files involved:** new picker composables; `core-ui/.../CountryChip.kt`; `core-ui/.../CountryChipsRow.kt`; all 8 Screens (to wire `onAdd`).

**Acceptance:** tapping `+ add` opens picker; selecting a country adds it to the active row and re-issues the query.

---

## P1 · Locale-aware number formatting + extract shared formatters

**Why:** every Screen has its own inline `formatLargeNumber()` / `formatPercent()` / etc. Inconsistent rounding, no locale awareness (PL uses `,` decimal, EN uses `.`). Per `CLAUDE.md` localization is a requirement.

**Steps:**
1. Create `core-ui/src/commonMain/kotlin/eu/eurostat/ui/format/NumberFormat.kt`.
2. Expose `formatLargeNumber(value: Long, locale: Locale = Locale.current): String` → "83.2 M", "1.2 B", "689 B €".
3. Expose `formatPercent(value: Double, decimals: Int = 1): String` → "15.0%", "−2.5%".
4. Expose `formatMonetary(valueMEur: Long): String` → "3 451 B €" with proper thousand-separator per locale.
5. Use `kotlinx-datetime` or platform APIs via `expect/actual` if needed for locale.
6. Migrate every Screen's inline formatter to call these. Delete locals.

**Files involved:** new `core-ui/.../format/NumberFormat.kt`; expect/actual if needed; all 8 Screens.

**Acceptance:** zero inline formatters in feature Screens. Numbers render correctly per locale.

---

## P2 · Tablet + desktop responsive layouts

**Why:** desktop target ALREADY builds (`./gradlew :composeApp:packageUberJarForCurrentOS` produces a 99 MB self-contained `.jar` that runs on macOS/Linux/Windows). Tablet works as Android-large / iPad. But every Screen is currently phone-first — a 13" desktop window shows a 360dp-wide column with empty space on each side. Doesn't crash, but looks wrong.

**Why this matters for funders:** "single codebase, three form factors" is a strong "European public interest" angle — journalists on desktop, students on tablet, citizens on phone, all from one repo. Already implemented at infrastructure level; just needs UI polish.

**Steps:**
1. Add `BoxWithConstraints` / `WindowSizeClass` detection at the root of each Screen.
2. For width ≥ 840dp (tablet landscape / desktop), render as two-pane master-detail:
   - Left pane (320dp): country picker + year scrubber + switcher
   - Right pane (rest): hero chart + KPI tiles
3. For width ≥ 1200dp (large desktop), add a third pane: data table view of the raw points.
4. Test on:
   - Android phone emulator (360dp portrait)
   - Android tablet emulator (840dp landscape)
   - macOS desktop window resized small→large
5. Reuse Material3 `WindowSizeClass` API — already a transitive dep via Compose Multiplatform.

**Files involved:** all 8 `*Screen.kt`, possibly new `core-ui/.../layout/AdaptiveLayout.kt` for the master-detail wrapper, `composeApp/desktopMain/Main.kt` for default window size (1280×800).

**Acceptance:** screens render correctly at 360dp / 600dp / 840dp / 1280dp window widths. No empty bands of paper. Desktop window resize is smooth (no layout snap-back).

**Effort estimate:** ~8-12 hours. Could be its own NLnet milestone (€3-5k as M5 or stretch on M4).

---

## P2 · Linux Flatpak + Windows installer + macOS .dmg packaging

**Why:** `packageUberJarForCurrentOS` only outputs `.jar`. For distribution to non-dev users, we need native installers.

**Steps:**
1. Use `compose.desktop { application { nativeDistributions { targetFormats(Dmg, Msi, Deb) } } }`.
2. Wire up GitHub Actions matrix build: `runs-on: [macos-latest, ubuntu-latest, windows-latest]`.
3. Sign macOS `.dmg` (requires Apple Developer cert) and Windows `.msi` (requires Windows code-signing cert) — optional, deferred until budget exists.
4. Publish artifacts to GitHub Releases on tag.

**Files involved:** `composeApp/build.gradle.kts` (nativeDistributions block), `.github/workflows/release.yml` (new).

**Acceptance:** push a tag → CI produces 3 native installers as release artifacts.

**Effort:** ~4-6 hours (excluding cert procurement).

---

## P2 · Comparison mode screen

**Why:** wireframes (`design/screens-compare.jsx`) define an overlay-lines or small-multiples view for comparing 2-3 countries on the same indicator. Currently the in-module screens only show one country at a time. Comparison is a flagship feature per the brief.

**Steps:**
1. New screen accessible from any feature screen via a "compare" action in `ModuleAppBar` (add icon).
2. Takes the current module + currently active country + lets user pick 1-2 more countries.
3. Renders `EurostatLineChart` with multiple `ChartSeries`, one per country in distinct accents.
4. Variant B: `EurostatSmallMultiples` of `EurostatLineChart`s — better for DE vs MT scale gap.
5. Toggle for "% of base year" normalization.

**Files involved:** new `feature-compare/` module or shared component in `core-ui/.../component/compare/`.

**Acceptance:** can compare GDP across DE/FR/PL on one chart with distinct colors and legend.

---

## P2 · Search & Settings screens

**Why:** wireframes (`design/screens-system.jsx`) define both. Settings exposes theme/language/default-country preferences; Search lets users find indicators across all 8 modules. Both are referenced in `ModuleAppBar` (search icon dispatches nowhere).

**Steps:**
1. Settings:
   - `feature-settings/` or live in `composeApp` since it's a single screen.
   - Items per wireframe: language (EN/DE/FR/PL/UK), default country, theme (system/light/dark), units, refresh-on-WiFi-only toggle, cache size + clear.
   - Persist via Multiplatform Settings (`com.russhwolf:multiplatform-settings`).
2. Search:
   - Indexes a static `INDICATORS.json` resource listing every indicator across the 8 modules (label, module, dataset code, description).
   - Returns top matches; tapping one navigates to the relevant module and sets the active metric switcher.

**Files involved:** new `feature-settings/`, `feature-search/`, search index resource, navigation wiring.

**Acceptance:** both screens reachable, settings persist across app restart, search returns sensible results.

---

## P2 · Bundle Inter + IBM Plex Mono fonts

**Why:** `EurostatTypography.kt` falls back to `FontFamily.SansSerif` / `Monospace`. Looks acceptable but not on-brand.

**Steps:**
1. Download Inter (Regular, Medium, SemiBold) + IBM Plex Mono (Regular, Medium) from Google Fonts.
2. Add files to `core-ui/src/commonMain/composeResources/font/`.
3. Set up Compose Resources in `core-ui/build.gradle.kts` (`compose.components.resources`).
4. In `EurostatTypography.kt`, replace `FontFamily.SansSerif` with `FontFamily(Font(Res.font.inter_regular, FontWeight.Normal), ...)`. Likewise for Plex Mono.

**Files involved:** `core-ui/build.gradle.kts`, font files, `EurostatTypography.kt`.

**Acceptance:** rendered text visibly uses Inter (compare against `design/index.html` which loads it via Google Fonts CDN).

---

## P2 · Translations: PL + UK strings

**Why:** `CLAUDE.md` lists EN/DE/FR/PL/UK. Eurostat returns EN/DE/FR labels natively. PL + UK need our translation. Currently every string is a hardcoded literal in Kotlin — no string resources.

**Steps:**
1. Set up Compose Resources string resources in `core-ui` (or `core-common`).
2. Extract every user-facing string from feature Screens + core-ui components into `strings.xml` (or Compose Resources `.xml`).
3. Translate EN → PL + UK.
4. Settings screen language picker switches Locale.

**Files involved:** all Screens, all core-ui components, new resource files.

**Acceptance:** language switcher in Settings changes UI language across all screens.

**Estimate:** ~200-300 strings. Big lift.

---

## P3 · Detail modal on chart tap

**Why:** per brief: "Tap a chart point → modal with exact value, year, units, source citation". Charts currently don't respond to taps.

**Steps:**
1. Add `onPointClick: ((ChartPoint) -> Unit)?` param to each chart type in `core-charts`.
2. Detect tap → calculate nearest data point in canvas coordinates.
3. Pop a `ModalBottomSheet` showing year + value + unit + dataset code.

---

## P3 · Pull-to-refresh

**Why:** users expect it on mobile data screens. Currently only the refresh icon in `ModuleAppBar` works.

**Steps:**
1. Wrap each Screen content in Material3 `PullToRefreshBox`.
2. Dispatch `XxxIntent.Refresh` on release.
3. Show indicator until state returns to `Content`.

---

## P3 · Better error messages

**Why:** `ErrorState` currently shows generic message. `AppError` is a sealed class — UI should map each subtype to a specific localized message.

**Steps:**
1. Add `@Composable fun AppError.localizedMessage(): String` extension somewhere in `core-ui`.
2. `NoNetwork` → "Check your connection"; `Http5xx` → "Eurostat servers are temporarily unavailable"; `Parse` → "Got unexpected data format"; etc.

---

## P3 · Performance: `remember` heavy chart computations

**Why:** `EurostatChoropleth` parses 23 SVG paths on every recomposition. `EurostatRadarChart` recomputes axis angles. `EurostatPyramidChart` rebuilds rects.

**Steps:**
1. Wrap heavy precomputations in `remember(inputs) { ... }`.
2. For Choropleth, `remember { parseAllCountryPaths() }` — paths are static.
3. Profile with Layout Inspector.

---

## P3 · CI

**Steps:**
1. GitHub Actions workflow on push: `./gradlew assembleDebug allTests`.
2. iOS smoke build on macOS runner if budget allows.

---

## P3 · Release config

**Steps:**
1. Signing config for Android release.
2. Proguard/R8 rules — likely needs additions for Decompose, Ktor, kotlinx.serialization.
3. iOS deployment target review.
4. App icons + adaptive icon for Android.

---

## How to use this document

- Start at P0. Don't pick P1 until all P0 are done.
- Each task has acceptance — don't mark done until it's verified.
- Tasks marked "agent-friendly" can be handed to an opus agent. Tasks involving device testing / Xcode / app store cannot.
- External code reviews (e.g. from NLnet-recommended audit firms) may shift priorities — adapt as findings come in.

**Definition of "100% working" for this app:**

1. ✅ Android APK installs and launches on a fresh device.
2. ⏳ iOS app builds and launches in simulator.
3. ⏳ All 8 feature tabs render real Eurostat data on first open.
4. ⏳ Refresh button + pull-to-refresh both work.
5. ⏳ Offline state shows cached data with clear stale indicator.
6. ⏳ Error states recover via retry button.
7. ⏳ Bottom-nav Overview opens a real landing screen.
8. ⏳ Country picker opens from `+ add` chip; selection adds country to comparison.
9. ⏳ Settings screen persists preferences across restart.
10. ⏳ All `allTests` pass.
11. ⏳ No `mock*()` calls remain in any feature Screen.
12. ⏳ No dead code (`WireframeApp`, legacy `core.ui` package, `Sketch*` primitives).

Currently 1/12. Target: 12/12.
