# EU Stats Multiplatform — project conventions for AI assistants

> Independent third-party open-source client for the public Eurostat HTTP API.
> Not affiliated with Eurostat or the European Commission. See README.md disclaimer.

## What this is

Kotlin Multiplatform app (Android · iOS · macOS · Linux · Windows desktop) that visualizes public European statistical data.
Multi-module Clean Architecture. **Phase 4 complete; Phase 5 in progress** — all 8 feature modules ship real public-API data through the design-system UI. Already landed from the Phase 5 list: the searchable country picker (`CountryPickerSheet`) and Unicode flag rendering, bundled Inter + IBM Plex Mono fonts, a wired (placeholder) `feature-settings` screen, and Android PL/UK string resources. Still ahead: Overview/landing screen, comparison mode, search, settings persistence, KMP-level (Compose Resources) localization + locale-aware number formatting, and the iOS toolchain. See `NEXT_STEPS.md`.

## Architecture

```
core-common      → Result<T>, AppError, DispatcherProvider
core-jsonstat    → JSON-stat 2.0 parser (fully implemented + tested)
core-network     → Shared Ktor HttpClient + EurostatApiClient
core-database    → SQLDelight schemas + DAOs
core-ui          → Compose Multiplatform design system
                   - theme/  → EurostatTheme + object Euro (colors, typography, spacing, shapes, moduleAccents)
                   - component/  → EuroCard, ModuleAppBar, CountryChip(sRow), CountryPickerSheet,
                                  YearScrubber, YearDropdown, MetricHeadline, StatTile, SourceFooter,
                                  StaleBanner, SegmentedControl, ChipRow, UnderlineTabs, PillToggle,
                                  MetricDropdown, KpiTileSelector, BottomTabBar (preserved, not rendered)
                   - component/states/  → LoadingShimmer, EmptyState, ErrorState
                   - layout/  → AdaptiveScaffold (WindowSizeClass-driven shell)
core-charts      → Pure Compose Canvas chart library (Koalaplot dropped)
                   - line, stacked-bar, pyramid, heatmap, diverging-bar,
                     radar, small-multiples, multi-line-highlighted
                   - model/  → ChartPoint, ChartSeries, ChartAxis, ColorScale
                   - Note: area, donut, sankey, choropleth, bar were
                     scaffolded earlier but removed in the pre-release
                     pass (zero call sites). Add back when a feature
                     screen actually needs them.
core-navigation  → Decompose root component + ChildConfig

feature-population, feature-economy, feature-environment, feature-trade,
feature-transport, feature-tourism, feature-social, feature-science
  → each follows the same data/domain/ui layering as feature-population.
  → Screens consume Euro.* tokens from core-ui and chart types from core-charts.
  → All 8 modules ship real Eurostat data with no hardcoded mocks.
feature-settings  → placeholder Settings screen wired into navigation
                   (ChildConfig.Settings); no preference persistence yet (Phase 5).

composeApp        → app shell: AdaptiveScaffold + Decompose Children stack.
                   Landing is HomeScreen (responsive 8-card module grid; initial
                   config ChildConfig.Home); tapping a card pushes the feature
                   screen. BottomTabBar exists but is not currently rendered.
```

## Tech stack

- **UI**: Compose Multiplatform
- **Navigation**: Decompose (NOT Voyager, NOT Compose Navigation)
- **Async**: Kotlin Coroutines + Flow + StateFlow
- **HTTP**: Ktor Client (Darwin engine on iOS, OkHttp on Android)
- **Serialization**: kotlinx.serialization
- **Cache**: SQLDelight
- **DI**: Koin
- **Charts**: Pure Compose `Canvas` (Koalaplot dropped — see `core-charts/`)
- **Testing**: kotlin.test in commonTest, Turbine for Flow assertions

## Core contracts — read these before writing anything

1. **`Result<T>`** — three states (Loading / Success / Error). Always wrap data layer return types as `Flow<Result<T>>`. Never throw across the data/domain boundary.

2. **`AppError`** — sealed taxonomy of failures. Repository maps exceptions to AppError. UI maps AppError to localized strings.

3. **`DispatcherProvider`** — never reference `Dispatchers.IO` or `Dispatchers.Default` directly. Inject this and use `dispatchers.io`, etc.

4. **Repository contract** — see `feature-population/.../domain/PopulationRepository.kt`. Stale-while-revalidate:
   - emit Loading first
   - emit cached data (if any) as Success(isStale=true)
   - emit fresh data as Success(isStale=false) on network success
   - emit Error only if no cache AND network failed
   - swallow network failures silently if stale cache was already emitted

5. **Component (Decompose)** — see `feature-population/.../ui/PopulationComponent.kt`. Pattern:
   - interface `XxxComponent` exposes `StateFlow<XxxUiState>` and `onIntent()`
   - sealed `XxxUiState` with Loading / Content / Empty / Error variants
   - sealed `XxxIntent` for all user actions
   - default impl receives `ComponentContext`, delegates lifecycle via `componentContext`

6. **JSON-stat parser** — `core-jsonstat/JsonStatParser.kt` is fully implemented. Don't reinvent. To use: call `parser.parse(response)` → `List<JsonStatCell>`, then adapt cells to your feature's domain model in the repository's `fromCells()` extension.

## Conventions

- Package root: `eu.eurostat.{module}` (e.g. `eu.eurostat.feature.population.domain`)
- One class per file; file name matches public class name
- Sealed types as `sealed interface`, not `sealed class`, unless state needs to be added
- Prefer extension functions over utility classes
- No `!!` operator anywhere — use `requireNotNull`, `checkNotNull`, or sealed Result handling
- KDoc on every public type, every interface method, every non-obvious algorithm

## Testing

- Unit tests in `commonTest` whenever possible (run on JVM, fast)
- Platform-specific tests in `androidUnitTest` / `iosTest` only when testing actual implementations
- Use Turbine for Flow testing: `flow.test { assertEquals(Loading, awaitItem()); ... }`
- Fake repositories implement the interface directly, not Mockk — KMP doesn't have Mockk on iOS

## Phase status

- [x] Phase 0 — JSON-stat parser + skeleton contracts
- [x] Phase 0 — Gradle setup (build-logic, version catalog, settings.gradle.kts)
- [x] Phase 1 — core-common, core-network, core-database, core-navigation
- [x] Phase 1 — **core-ui** (full design system: tokens + 18 components + 3 state composables + AdaptiveScaffold + EurostatTheme with `object Euro` accessor)
- [x] Phase 1 — **core-charts** (8 chart types on Compose Canvas; Koalaplot removed)
- [x] Phase 2 — **feature-population** end-to-end (real `demo_pjangroup` with 18 age cohorts + M/F → pyramid hero)
- [x] Phase 3 — **feature-economy** (3 parallel datasets: GDP/HICP/deficit)
- [x] Phase 3 — **feature-environment** (3 parallel datasets with sector breakdown; cache + commonTest restored)
- [x] Phase 4 — **feature-trade** (`ext_lt_intratrd` exp/imp/balance → diverging bars)
- [x] Phase 4 — **feature-transport** (road + air parallel → small multiples; sea disabled)
- [x] Phase 4 — **feature-tourism** (DOM/FOR/TOTAL nights → stacked bars; seasonality heatmap driven by real `tour_occ_nim`)
- [x] Phase 4 — **feature-social** (3 parallel % datasets → KPI-tile-driven highlight line chart)
- [x] Phase 4 — **feature-science** (3 parallel % datasets → radar + 3 sparklines)
- [x] Phase 4 — **composeApp** wired to new `EurostatTheme`; HomeScreen 8-card grid + Decompose stack (BottomTabBar preserved but not rendered). APK assembles (19 MB).
- [ ] **Phase 5** — Overview dashboard / landing screen (no Overview destination exists yet; current landing is the HomeScreen 8-card grid)
- [~] **Phase 5** — Country picker (searchable `CountryPickerSheet` ships inline on feature screens; tap-the-map variant deferred)
- [ ] **Phase 5** — Comparison mode (multi-country overlay)
- [~] **Phase 5** — Search & Settings screens (Settings wired as a placeholder, no persistence yet; Search not started)
- [ ] **Phase 5** — Cache strategy convergence (JSON-blob column for multi-dim models)
- [x] **Phase 5** — Bundle Inter + IBM Plex Mono fonts (loaded from `composeResources/font/`)
- [x] **Phase 5** — Real flag rendering (`flagFor()` Unicode emoji in core-common) + Android PL/UK string resources
- [ ] **Phase 5** — KMP-level (Compose Resources) PL/UK localization + locale-aware number formatting
- [ ] **Phase 5** — Fix iOS toolchain (`xcrun xcodebuild` exits 72); verify iosApp builds + runs in simulator
- [ ] **Phase 6** — Verified on Android device + iOS simulator; CI; release config; signing

See `NEXT_STEPS.md` for the prioritized punch list with file refs and acceptance criteria.

## Eurostat dataset codes per feature

All codes + filter values verified against live Eurostat API. Do not edit without re-checking.

| Feature | Dataset | Key filters that produce data |
|---|---|---|
| population  | `demo_pjangroup`   | `sex=T/M/F`, `age=TOTAL+18 5-yr cohorts (Y_LT5..Y_GE85)` — NOT `demo_pjan` (per-year only) |
| economy     | `nama_10_gdp`      | `unit=CP_MEUR`, `na_item=B1GQ`                                      |
| economy     | `prc_hicp_aind`    | `unit=INX_A_AVG`, `coicop=CP00`  *(NOT `I15`)*                       |
| economy     | `gov_10dd_edpt1`   | `unit=PC_GDP`, `na_item=B9`, `sector=S13`                            |
| environment | `env_air_gge`      | `airpol=GHG`, `src_crf=TOTX4_MEMO/CRF1A3/CRF1A2`, `unit=MIO_T`       |
| environment | `nrg_bal_c`        | `siec=TOTAL`, `nrg_bal=FC_E/FC_TRA_E/FC_IND_E`, `unit=KTOE`          |
| environment | `sdg_13_10`        | `unit=I90`  *(NOT `I15`)*                                            |
| trade       | `ext_lt_intratrd`  | `indic_et=MIO_EXP_VAL/MIO_IMP_VAL/MIO_BAL_VAL`, `sitc06=TOTAL`  (no `unit` dim) |
| transport   | `road_pa_buscoa`   | `unit=THS_PAS`, `tra_cov=TOTAL` (NO `vehicle` — dim not defined; ×1000 in mapper) |
| transport   | `avia_paoc`        | `unit=PAS`, `tra_meas=PAS_CRD`, `tra_cov=TOTAL`, `schedule=TOT` (NO `partner` — dim not defined) |
| transport   | (sea: disabled)    | `mar_pa_aa` uses port-based dim, not `geo` — not called              |
| tourism     | `tour_occ_ninat`   | `c_resid=DOM/FOR/TOTAL`, `unit=NR`, `nace_r2=I551-I553`              |
| tourism     | `tour_dem_tttot`   | `unit=NR`, `purpose=TOTAL`, `duration=N_GE1`, `c_dest=WORLD` (NOT `partner`; dim is named `c_dest`) (NO `c_resid` dim) |
| tourism     | `tour_occ_nim`     | `c_resid=TOTAL`, `unit=NR`, `nace_r2=I551/I552/I553`, monthly `time=YYYY-MM` (monthly nights → seasonality heatmap) |
| social      | `ilc_li02`         | `unit=PC, statinfo=MED_EI, rskpovth=B_60, sex=T, age=TOTAL` → `povertyRate`  *(NOT `indic_il` — removed upstream)* |
| social      | `ilc_peps01n`      | `unit=PC, sex=T, age=TOTAL` (NO `indic_il` — dim not defined) → `atRiskRate`  *(NOT `ilc_peps01` — frozen @2020)* |
| social      | `hlth_silc_01`     | `levels=VGOOD`, `sex=T`, `age=Y_GE16`, `wstatus=POP` → `healthSatisfaction` |
| science     | `rd_e_gerdtot`     | `unit=PC_GDP`, `sectperf=TOTAL`                                      |
| science     | `isoc_ci_ifp_iu`   | `unit=PC_IND`, `ind_type=IND_TOTAL`, `indic_is=I_IU3`                |
| science     | `edat_lfse_03`     | `unit=PC`, `isced11=ED5-8`, `sex=T`, `age=Y25-64`                    |

Residence/sector code gotchas:
- `c_resid=FOR` for "international/foreign" (NOT `INTL`)
- `src_crf=TOTX4_MEMO` for "total GHG" (NOT `TOTAL`)
- `indic_et` values carry the unit (e.g. `MIO_EXP_VAL`); do not also send a `unit` filter
- `sitc06=TOTAL` is required for `ext_lt_intratrd`: without it the API returns 24 rows (one per SITC product category) instead of 3, causing last-cell-wins mapping errors
- `nrg_bal=FC_E` is the working total final consumption (NOT `FC` or `TOTAL`)
- `tour_dem_tttot` uses `c_dest` for destination (NOT `partner` — `partner` is for trade datasets)
- `ilc_li02` no longer has an `indic_il` dimension (Eurostat restructure; pinning `indic_il=LI_R_MD60` now returns HTTP 400). The at-risk-of-poverty rate is sliced as `statinfo=MED_EI` (median equivalised income) + `rskpovth=B_60` (below 60% of the median). Pin both — `statinfo` has 2 categories and `rskpovth` has 8 — or the mapper last-cell-wins.
- `ilc_peps01` is frozen at 2020 (old AROPE methodology, empty `value{}` for later years) — use `ilc_peps01n` (new AROPE definition, data from ~2015, actively updated). Same `unit=PC, sex=T, age=TOTAL` filters; still no `indic_il` dimension.
- `ext_lt_intratrd` has a `partner` dimension (`EU27_2020` / `EXT_EU27_2020` / `WORLD`) — the code pins `partner=EU27_2020` via `TradeQuery.partner`. Leaving it unpinned returns 3 rows per period → last-cell-wins in `TradeCellMapper` (which groups by geo+time only).

API base: `https://ec.europa.eu/eurostat/api/dissemination/statistics/1.0/data/{dataset_code}?format=JSON&lang=EN`

## How to add a new feature module

1. Copy `feature-population/` structure
2. Replace domain model (e.g. `PopulationDataPoint` → `GdpDataPoint`)
3. Replace dataset code in API service
4. Adapt `JsonStatCell` → domain model in the repository's mapper function
5. Build UI screen using the same Component + UiState + Intent pattern
6. Write unit tests for the new repository and component
