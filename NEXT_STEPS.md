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

**Update (2026-09-20, ✅ emulator walk-through done, two passes):** all 8 modules were walked on an API 36 emulator in EN, PL and UK — live data, no crashes — and offline (with/without cache, retry), rotation, forced refresh, Search, Compare, Settings persistence and a wide-screen layout were exercised. 16 defects were found and fixed (notably `avia_paoc` was queried with `schedule=TOT` instead of `TOTAL`, so Transport air data never loaded); see [`RUN_REPORT.md`](RUN_REPORT.md). **Still open:** a run on a physical device, an iOS run of the offline-error mapping (`core-network/.../UrlErrorClassification.kt` is JVM-tested; the iOS `actual` that calls it is compiled only by the CI iOS job, not yet built locally), process-death restore, and the smaller leftovers listed under "Found, not fixed" in the report.

**Files involved:** none to edit. Pure verification.

**Acceptance:** all 8 tabs render real data without crash on at least one Android device. Any crashes become P0 bug tickets.

---

## P0 · Fix iOS toolchain so iosApp can build

**Why:** `xcrun xcodebuild -version` returns exit 72 — Xcode CLI not properly configured. Every agent's `:build` failed at `linkDebugTestIosX64`. iOS half of "Kotlin Multiplatform" is unverified.

**Update:** the KMP common code now *compiles* for iOS Native — `./gradlew :composeApp:compileKotlinIosSimulatorArm64` is green after removing a JVM-only `toSortedSet` in `feature-environment` (commit `282acb5`). The remaining blockers are the Xcode toolchain (`xcrun` exit 72) and the `linkDebug*Ios*` / `iosApp.xcodeproj` wrapper — not the Kotlin sources.

**Update 2 (v0.4.0 release prep, ✅ RESOLVED — app runs in the simulator):**
two independent blockers were found and fixed:
1. *Toolchain*: `xcode-select -p` pointed at `/Library/Developer/CommandLineTools` while a full Xcode 26.2 sits in `/Applications/Xcode.app`. Workaround without touching system state: prefix builds with `DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer`. Permanent machine fix (needs admin password, once): `sudo xcode-select -s /Applications/Xcode.app/Contents/Developer`.
2. *Project*: `iosApp.xcodeproj/project.pbxproj` was a 1.2 KB stub placeholder (see the old iosApp/README.md) — `xcodebuild` failed with "Unable to read project". A real project is now generated from `iosApp/project.yml` via XcodeGen and checked in, with three integration fixes: `JAVA_HOME` pinned in the gradle pre-build phase (Xcode scrubs PATH → JDK mismatch corrupted Kotlin incremental caches), `-lsqlite3` linked (static framework + SQLDelight native driver), `CADisableMinimumFrameDurationOnPhone` added to Info.plist (Compose PlistSanityCheck aborts without it), and the Swift call corrected to `KoinIOSKt.doInitKoinIos()`.

**Verified:** `xcodebuild … build` → BUILD SUCCEEDED; app installs and launches on the iPhone 17 simulator (iOS 26.2) and renders the Overview dashboard with live Eurostat data (hero GDP + all 8 module teasers). Remaining: walk all 8 tabs interactively (step 6 below) — same as the Android on-device smoke run.

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

## ✅ DONE · Add an Overview / landing dashboard screen

**Status:** ✅ Done (code + unit tests; on-device visual check still pending). Shipped
as the `feature-overview` module: `DefaultOverviewComponent` observes all 8 feature
repositories concurrently (Koin singletons) and `combine()`s them into `OverviewUiState`
— one live teaser metric per module for the default country, each degrading
independently. `OverviewScreen` renders a hero GDP headline + a responsive per-module
teaser grid and is bound to `ChildConfig.Home` (the old static `HomeScreen` grid was
removed). `OverviewComponentTest` covers the aggregation with 8 fake repositories.
Original task notes kept below for reference.

**Why:** the app currently opens on `HomeScreen` — a responsive 8-card module grid (`composeApp/src/nativeAppMain/.../home/HomeScreen.kt`), launched via `ChildConfig.Home` (the initial config in `RootComponent`). There is **no** `Overview` destination, `feature-overview` module, or `BottomTabDestination.Overview` member — the design brief's Overview (hero stat + per-module teaser metrics) is not built yet. The HomeScreen grid is a functional landing but not the data-rich dashboard the brief describes.

**Steps:**
1. Decide: enrich `HomeScreen` into the dashboard, OR add a dedicated `ChildConfig.Overview` + `feature-overview` aggregator module (copy `feature-population/` without a data layer). Pick the cleaner of the two.
2. Implement the dashboard matching `design/screens-overview.jsx` `ScreenOverviewA` (hero metric + 2-column module tile grid). For each tile, show a teaser metric from the respective feature repository, or a static label + icon if cross-module wiring is too coupled.
3. If a new destination: add it to `ChildConfig.kt`, the `RootComponentFactory`, and `composeApp/.../EurostatApp.kt`'s `Children` dispatcher; set it (or keep `Home`) as the initial config.

**Files involved:** `composeApp/src/nativeAppMain/.../home/HomeScreen.kt`, `core-navigation/.../ChildConfig.kt`, `core-navigation/.../RootComponent.kt`, `composeApp/.../EurostatApp.kt`, optional new `feature-overview/` module + `settings.gradle.kts`.

**Acceptance:** the landing screen shows a hero stat + per-module teaser metrics, not just a static grid.

---

## ✅ DONE (v0.4.0) · Environment commonTest restored

**Status:** ✅ Done. `feature-environment/src/commonTest/` has 4 test classes — `EnvironmentApiServiceImplTest`, `EnvironmentCellMapperTest`, `EnvironmentRepositoryImplTest`, `EnvironmentComponentTest` — plus fakes, all green in the 499-test suite. Original task notes kept below for reference.

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

## ✅ DONE · Cache strategy convergence

**Status:** ✅ Done. `MultiDimCacheEntity` (JSON-blob, `core-database`) + `BlobCacheStore`/`JsonBlobCache<T>` (`core-common`, corrupted JSON degrades to a miss) + `SqlDelightBlobCacheStore` bound in Koin. Population cohort frames are cached (pyramid works offline; flat totals table kept as fallback tier); tourism moved off the residence-tall table and its `-1` sentinel to one blob per query (points + labels + seasonality heatmap). SQLDelight migrations infrastructure added (`migrations/1.sqm`, `2.sqm` → schema v3, `SchemaMigrationTest`); the desktop driver is schema-managed (fixes an unconditional-`Schema.create()` crash on second launch). Blob reads/writes are `safeFetch`-guarded so storage failures degrade to misses instead of escaping the flow; empty-points blobs revalidate instead of acting as 12h negative cache. Environment keeps its working sector-tall cache (already standard). Original notes below.

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

## ✅ DONE · Remove dead code

**Status:** ✅ Done (pre-release pass). `rg "Sketch|Wireframe"` returns no Kotlin hits; the legacy `eu.eurostat.core.ui.*` package and `WireframeApp.kt` are gone. Original notes below.

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

## ✅ DONE · Country picker screen + real flag rendering

**Status:** ✅ Done. `CountryPickerSheet` (searchable multi-select) ships in core-ui and every feature screen wires the "+ add" chip to it; selection re-issues the query via `SelectCountries` intents. Flags now render from 33 bundled circle-flags (HatScripts, MIT) vector drawables via `CountryFlag` in core-ui (used by `CountryChip`, `CountryPickerSheet` and the Settings default-country row); the Unicode-emoji `flagFor()` in core-common remains only as the fallback for flagless aggregates such as `EA20`. Tap-the-map variant deferred (choropleth was removed with zero call sites). Original notes below.

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

## ✅ DONE · Locale-aware number formatting + extract shared formatters

**Status:** ✅ Done. `core-ui/.../format/NumberFormat.kt` (`eu.eurostat.ui.format`): formatDecimal / formatSignedDecimal / formatPercent / formatSignedPercent / formatLargeNumber(Parts) / formatBillionsFromMillions / formatGrouped. Decimal + grouping separators come from the platform locale via expect/actual (java.text on Android/desktop, NSNumberFormatter on iOS); ties round away from zero; magnitude buckets promote on rounding rollover (999_972 → "1.0 M"). All 8 screens migrated — only thin domain-suffix wrappers remain. 29-case commonTest. Original notes below.

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

## 🟡 MOSTLY DONE · Tablet + desktop responsive layouts

**Status:** implemented via a shared three-slot `AdaptiveTwoPane` in core-ui (exact phone ordering <840dp through the compact slot; ≥840dp renders a 320dp controls pane + content pane), adopted by all 8 feature screens; desktop window opens at 1280×800. Unit suite + APK + iOS compiles green; desktop app ran at 1280×800 with no runtime exceptions. **Remaining:** the visual pass at 360/600/840/1280 (needs macOS Screen Recording permission for the assistant, or a manual look) — treat as part of the demo/RUN_REPORT week. Original notes below.


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

## 🟡 MOSTLY DONE · Linux Flatpak + Windows installer + macOS .dmg packaging

**Why:** `packageUberJarForCurrentOS` only outputs `.jar`. For distribution to non-dev users, we need native installers.

**Status:** `composeApp/build.gradle.kts` now has a `nativeDistributions` block (`targetFormats(Dmg, Msi, Deb)`, package name "EU Stats", bundleID `eu.eurostat.app`, AGPL `LICENSE` bundled) with per-OS icons generated from the store icon (`app.icns` via `sips`+`iconutil`, `app.ico` via Pillow, `app.png` for Linux) in `composeApp/icons/`. `:composeApp:packageDmg` verified locally → `EU Stats-1.0.0.dmg` (122 MB). Local gotcha: Android Studio's bundled JBR has no `jpackage` — point `JAVA_HOME` at a full JDK (e.g. the Gradle-provisioned Temurin under `~/.gradle/jdks`) for any `package*` task.

**Steps still open:**
1. Verify `packageMsi` / `packageDeb` — these only build on their native OS, so they need a CI matrix run (not yet added) rather than the maintainer's single macOS machine.
2. Sign macOS `.dmg` (requires Apple Developer cert) and Windows `.msi` (requires Windows code-signing cert) — deferred to post-grant per `NLNET_SUBMISSION/03-milestones.md`.
3. Publish artifacts to GitHub Releases on tag (see the CI release-workflow item above).
4. Flatpak manifest for Flathub — not started.

**Acceptance:** push a tag → CI produces native installers as release artifacts. (Dmg build + icons verified locally; Msi/Deb + the tag-triggered publish step remain.)

---

## ✅ DONE · Comparison mode screen

**Status:** ✅ Done (in-module variant). The economy hero chart overlays every picked country (CountryPickerSheet) with index-stable `SeriesPalette` colors + legend, and gains an "Absolute / Indexed 100" normalization toggle — each series rebased to 100 at its first point inside the visible year range (pure `rebaseToIndex()`, 9 tests; zero/missing-first series dropped, gaps preserved). Acceptance met: DE/FR/PL GDP on one chart with distinct colors and legend.

**Dedicated cross-module compare screen — ✅ DELIVERED.** `feature-compare` ships a full Component/UiState/Intent/Screen: pick one of 8 headline indicators (population, GDP, GHG, exports, air passengers, tourism nights, at-risk-of-poverty, R&D %GDP) and 2-5 countries via `CountryPickerSheet`; `CompareDataSource` adapts all 8 feature repositories to one `CompareSeries` shape (transport is strictly air-only — no road fallback, unlike the Overview teaser); one palette-colored line per country overlaid on `EurostatLineChart` with the `rebaseToIndex` Absolute/Indexed-100 toggle (now public in `core-charts`; feature-economy's internal duplicate deleted). Selection order is preserved so `SeriesPalette` colors stay stable as countries are added/removed; `ChildConfig.Compare` is wired like `feature-search`; entry is a compare pill on the Overview header. EN/PL/UK localized (31 keys). Built by an agent, adversarially reviewed through 4 lenses (data correctness, lifecycle, UI/l10n, Native safety) with all 9 findings fixed. Unit-tested including `iosSimulatorArm64Test`.

Original notes below.

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

## ✅ DONE · Search & Settings screens

**Status:** Both done. Settings: `PreferenceEntity` (SQLDelight, `2.sqm` migration) + `AppPreferences` Flows; theme (system/light/dark) applied app-wide through `EurostatTheme`; functional Clear-cache wiping all 8 cache tables in one transaction; settings gear on the Overview header; **default-country now seeds all 9 components' first query** (read once before the first fetch; mid-session changes apply on next start). Search: `feature-search` module — compiled-in index of 27 indicators across the 8 modules with tiered ranking (label prefix > word prefix > substring > keyword > description) and browse-by-module on blank query; search pill on the Overview header; opening a result replaces Search on the stack with the target module (Search is transient), so Back returns to where it was opened. 17 ranking/component tests.

**Header search icon:** now wired on Population, Economy, Environment, Social, Science and Compare (optional `onSearch` parameter, hooked up in `EurostatApp.kt` to push Search on top of the current module; Search's Back returns to it — `RootComponentTest` covers it). Trade, Transport and Tourism show no search icon; Search is still reachable from the Overview header pill. The dead `ModuleAppBar` "More" pill was removed.

**Remaining follow-up:** none — the *language* preference is now applied at runtime (see the Translations section below).

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

## ✅ DONE · Bundle Inter + IBM Plex Mono fonts

**Status:** ✅ Done. Inter (Regular/Medium/SemiBold) + IBM Plex Mono (Regular/Medium) live in `core-ui/src/commonMain/composeResources/font/` and `EurostatTypography` loads them via Compose Resources. Original notes below.

**Why:** `EurostatTypography.kt` falls back to `FontFamily.SansSerif` / `Monospace`. Looks acceptable but not on-brand.

**Steps:**
1. Download Inter (Regular, Medium, SemiBold) + IBM Plex Mono (Regular, Medium) from Google Fonts.
2. Add files to `core-ui/src/commonMain/composeResources/font/`.
3. Set up Compose Resources in `core-ui/build.gradle.kts` (`compose.components.resources`).
4. In `EurostatTypography.kt`, replace `FontFamily.SansSerif` with `FontFamily(Font(Res.font.inter_regular, FontWeight.Normal), ...)`. Likewise for Plex Mono.

**Files involved:** `core-ui/build.gradle.kts`, font files, `EurostatTypography.kt`.

**Acceptance:** rendered text visibly uses Inter (compare against `design/index.html` which loads it via Google Fonts CDN).

---

## ✅ DONE · Translations: PL + UK strings

**Why:** `CLAUDE.md` lists EN/DE/FR/PL/UK. Eurostat returns EN/DE/FR labels natively. PL + UK need our translation. Previously most strings were hardcoded literals in Kotlin.

**Status:** ✅ Done. All 9 remaining modules (economy, environment, trade, transport, tourism, social, settings, search, overview) extracted the same way as core-ui/population/science — per-module `composeResources/values{,-pl,-uk}/strings.xml` with a plugin-derived `Res` package, screens resolving via `stringResource(...)`. Every module was adversarially reviewed for translation quality and key parity. `feature-search`'s `SearchModule.displayName` and `feature-overview`'s `ModuleTeaser.title/unit` moved from plain `String` to `StringResource` so they localize too. AppError messages also localize now (`AppError.localizedMessage()` in core-ui, EN/PL/UK; the old English-only `AppError.toUserMessage()` is deleted). The *language* preference (`AppPreferences.language`) is now applied at runtime via a `LocalAppLocale` expect/actual (Android Configuration swap, desktop JVM-locale composition local, iOS `AppleLanguages` override in `NSUserDefaults`) — `EurostatApp` observes the preference and `key()`-rebuilds the themed subtree, so the Settings language picker switches the UI language immediately, no restart needed.

**Acceptance met:** language switcher in Settings changes UI language across all screens immediately.

**Country names** are localized too (`countryDisplayName`, 34 `country_*` strings; the picker search also matches the localized name), as are the `ModuleAppBar` icon descriptions (`ui_action_*`). The country picker now sorts by the localized name (`CountryNameOrder`) and the Settings default-country label and the Compare legend use localized names too.

**Still open:** localizing the shared `SourceFooter` "fresh/stale" word is a nice-to-have, not blocking; native-speaker PL review of the author's own-knowledge translations is still worth doing before v1.0.

---

## ✅ DONE · Detail modal on chart tap

**Status:** ✅ Done (line charts), scoped to Economy, Environment and Compare. `EurostatLineChart` gained an opt-in `onPointTap: ((ChartSeries, ChartPoint) -> Unit)? = null` — null attaches no pointer input, so existing call sites are unchanged. A `Modifier.pointerInput` reuses the *exact* draw-pass value→pixel projection (hoisted into shared `mapXpx`/`mapYpx`) and calls a pure, unit-tested `nearestChartPoint(...)` in `core-charts/model` that picks the closest point within a 24.dp radius, skipping `null` gaps with a deterministic tie-break (10 test cases: nearest wins, threshold excludes far taps, null gaps skipped, empty input, non-identity projection, ties). Tapping opens `core-ui`'s new `ChartPointDetailSheet` (Material3 `ModalBottomSheet`) showing series label, year, formatted value, unit and dataset code — labels localized EN/PL/UK (`ui_chart_detail_*`), values pre-formatted by the feature module. Wired on the Economy hero chart and the Compare chart, both resolving the true **absolute** value for the tapped country+year even in Indexed-100 mode. Left unwired by design: Social (uses `EurostatMultiLineHighlighted`, a separate composable that does not share the line chart's internals) and Science (only a decorative 28dp `hideAxis` sparkline; its primary chart is the radar); bar/pyramid/heatmap/radar are out of scope. **Environment is now wired too** (hero chart; the sheet shows the country, sector, year, value formatted like the headline, unit and dataset code `env_air_gge` / `nrg_bal_c` / `sdg_13_10`). **Still not wired:** Transport's small-multiples panels (`hideAxis = true`), Social (`EurostatMultiLineHighlighted`, a separate composable) and Science (decorative sparklines; primary chart is the radar) — left out deliberately.

---

## ✅ DONE · Pull-to-refresh

**Status:** ✅ Done. Material3 `PullToRefreshBox` wraps the state-branch content on all 8 feature screens, dispatching the existing `Refresh` intents; the indicator follows the `Loading` state.

---

## 🟡 MOSTLY DONE · "Refresh failed" hint (9 of 10 screens)

**Problem:** a manual refresh that fails while a within-TTL cache exists is swallowed, so the footer still says "fresh" and the user cannot tell the refresh did not happen (RUN_REPORT, "Found, not fixed"). Repositories already throw from `refresh()`, so the signal reaches every component; nothing shows it.

**Status:** done in `core-ui` and in Population, Economy, Environment, Trade, Transport, Tourism, Social, Science and Compare, each with component tests that pass on Android and on Kotlin/Native. Checked on the API 36 emulator for Economy, Trade and Compare (Compare in Ukrainian): airplane mode + warm cache + refresh icon gives an orange dot and the localized "refresh failed · showing saved data" (the request takes several seconds to fail), and going back online + refresh returns the footer to "fresh". The English and Ukrainian text wraps to two lines at phone width and fits; the Polish copy was not looked at and none of the translations has had a native-speaker review. **Compare:** any one failing indicator repository counts as a failure (`CompareDataSource.refresh` already rethrows the first failure, all 8 repositories were read). **Not done:** Overview's footer is the static "live open data" string and its refresh does not force a network fetch. **Known edge:** Population's cohort query can emit a non-stale cache and then a non-stale network result, so the hint can stay next to fresh data after a failed refresh until the next load or query change.

**Design (no change to `Result<T>`, `AppError` or any repository):**
- `core-ui` `SourceFooter`: new `refreshFailed: Boolean = false`. When true the dot turns warn-coloured and the staleness text becomes a new `ui_footer_refresh_failed` string ("refresh failed · showing saved data", plus PL/UK). It also replaces Science's hard-coded `"fresh"`.
- Each of the 9 refresh-capable modules (Population, Economy, Environment, Trade, Transport, Tourism, Social, Science, Compare): `Content.refreshFailed: Boolean = false`; the component keeps a private flag, set from `runCatching { refresh() }.isFailure` in the `Refresh` handler and cleared by every other `load()` (query change, Retry) and by a stale emission; the screen passes it to `SourceFooter` (Trade and Transport need one extra `ContentBody` parameter).
- About 39 files in total, each edit mechanical. Suggested rollout: `core-ui` + Economy as a pilot, verify on the emulator (airplane mode, warm cache, pull-to-refresh), then replicate.

**Open decisions:** whether to keep the footer or use a banner strip (the pilot chose the footer); PL/UK wording review by a native speaker; whether the hint should also cover a failed background revalidation (today only the manual gesture).

**Adjacent polish, separate from this:** Population's footer always prints "fresh" even when stale (only the dot changes); the Overview offline banner is tappable but does not say it retries.

---

## ✅ DONE · Better error messages

**Status:** ✅ Done, and since localized. Originally shipped as a shared `AppError.toUserMessage()` in core-common (plain English string) with per-subtype copy (NoNetwork / 5xx vs other HTTP / Parse / CacheEmpty / Unknown), used by all 8 feature components (six of which previously surfaced raw `cause.toString()`). Now superseded: UI states carry the raw `AppError`, and a `@Composable AppError.localizedMessage()` in core-ui (EN/PL/UK) resolves the copy at the `ErrorState` call site, so error text follows runtime language switches; `AppError.toUserMessage()` is deleted as dead code.

---

## ✅ DONE · Performance: `remember` heavy chart computations

**Status:** ✅ Done. Pyramid/diverging max sides, stacked-bar max, radar axis angles, and line/multi-line axis bounds + ticks are hoisted into `remember(inputs)`; heatmap/small-multiples reviewed, no win. (Choropleth no longer exists — removed pre-release.) Layout-Inspector profiling deferred to on-device verification.

---

## 🟡 MOSTLY DONE · CI

**Status:** `.github/workflows/build.yml` now also triggers on push to `develop-v*` (previously only `main`/`master`) with three jobs: `android` (`assembleDebug` + `allTests` + `assembleRelease`, exercising the real R8/proguard pass, falling back to the debug keystore without secrets), `desktop` (`packageUberJarForCurrentOS` smoke test on Ubuntu), and `ios-test` (the real `iosSimulatorArm64Test` suite, not a compile-only gate — gated behind `android` since macOS runners bill ~10x). Remaining: a tag-triggered release workflow that builds and uploads the native installers (APK, Dmg, Msi, Deb) as GitHub Release artifacts.

**Update:** the tag-triggered release workflow exists (`.github/workflows/release.yml`: on a `v*` tag it builds the release APK — signed when the `KEYSTORE_*`/`KEY_*` secrets are set, debug-signed otherwise — plus the Dmg/Msi/Deb installers on a macOS/Windows/Ubuntu matrix and attaches them to the GitHub Release).

**Steps still open:**
1. Run it once for real: no `v*` tag has ever been pushed, so the Msi and Deb jobs have never executed (only `packageDmg` was verified, locally).
2. Configure the four signing secrets for a real release key.

---

## 🟡 MOSTLY DONE · Release config

**Status:** iOS app icon (`AppIcon.appiconset`, single-size 1024 no-alpha, wired via `project.yml`/XcodeGen) and desktop per-OS icons (`.icns`/`.ico`/`.png` generated from the store icon) are done; version sync landed (`versionCode` 60 / `versionName` 0.6.0 across Android, the Settings About screen, and iOS `Info.plist`); Proguard/R8 is now exercised in CI via `assembleRelease` on every push (see CI above), not just a manual step.

**Steps still open:**
1. Real Android release signing keystore (currently the documented debug-keystore fallback in `docs/RELEASING.md`).
2. iOS deployment target review.
3. F-Droid recipe: `metadata/eu.eurostat.app.yml` still pins `versionName` 0.4.0 / `versionCode` 40 / `commit: v0.4.0`, and no `v*` tag exists in the local repo — update it (and the `CurrentVersion*` lines) to the tag cut for the next release before filing the fdroiddata merge request.

---

## How to use this document

- Start at P0. Don't pick P1 until all P0 are done.
- Each task has acceptance — don't mark done until it's verified.
- Tasks marked "agent-friendly" can be handed to an opus agent. Tasks involving device testing / Xcode / app store cannot.
- External code reviews (e.g. from NLnet-recommended audit firms) may shift priorities — adapt as findings come in.

**Definition of "100% working" for this app:**

1. ✅ Android APK installs and launches on a fresh device.
2. ✅ iOS app builds AND launches in the simulator (real XcodeGen project); all 8 modules walked on an iPhone 17 simulator (iOS 26.5, Xcode 26.6) on 2026-09-20 and the Kotlin/Native suite passes locally (673 tests) — see `RUN_REPORT.md`. Physical iPhone / TestFlight still pending.
3. ✅ All 8 feature tabs render real Eurostat data on first open (Android emulator, EN/PL/UK — see `RUN_REPORT.md`; physical device and iOS walk-through still pending).
4. ✅ Refresh button + pull-to-refresh both work (pull-to-refresh on all 8 screens).
5. ✅ Offline state shows cached data with stale indicator (incl. cohort pyramid + tourism heatmap via the blob cache).
6. ✅ Error states recover via retry button, with per-subtype localized messages (`AppError.localizedMessage()`, EN/PL/UK).
7. ✅ Landing is the Overview dashboard (hero + live per-module teasers).
8. ✅ Country picker opens from `+ add`; multi-selection overlays the economy chart (distinct `SeriesPalette` colors + Indexed-100 toggle).
9. ✅ Settings persist across restart (SQLDelight `PreferenceEntity`; theme applied app-wide).
10. ✅ Unit suite green (`testDebugUnitTest` + desktop tests; 600+ tests incl. migrations).
11. ✅ No `mock*()` calls remain in any feature Screen.
12. ✅ No dead code (`WireframeApp`, legacy `core.ui` package, `Sketch*` primitives all removed).

Currently 12/12 on the Android emulator and the iPhone 17 simulator. Still open: a physical-device run (Android and iPhone/TestFlight), the offline / rotation checks on iOS, and keeping the CI `ios-test` job green: it used to OOM the Kotlin/Native compiler while caching `material-icons-extended`; this branch replaced that dependency with nine bundled vectors (`EuroIcons`), after which `ios-test` passed on CI (~24 min) and the Native link plus all tests take 45 s locally at the default heap.
