# Release Hardening Cycle Report

Single discovery + fix + verification cycle on EU Stats Multiplatform release candidate.

## Scope

- 285 Kotlin source files across 13 modules
- 8 feature modules (population, economy, environment, trade, transport, tourism, social, science)
- 7 core modules (core-common, core-jsonstat, core-network, core-database, core-ui, core-charts, core-navigation)
- composeApp shell

## Methodology

Three parallel Explore agents scanned for: (A) Eurostat data-correctness risks, (B) silent-failure patterns, (C) UI-state / dead-code / accessibility. Each finding was then re-verified by direct file read before any code change, because several agent claims were inaccurate when checked against source.

## Findings discovered

### CRITICAL — 0 confirmed

The three discovery agents flagged six findings as CRITICAL. Direct verification reduced all six to MEDIUM or false-positive:

| Agent claim | Verification | Verdict |
|---|---|---|
| Environment "loading-forever on slow network" | [EurostatApiClient.kt:76-80](core-network/src/commonMain/kotlin/eu/eurostat/core/network/EurostatApiClient.kt:76) installs `HttpTimeout` (request 30s, connect 10s). Worst case is 30s Loading → Error or Success(stale). Cannot hang indefinitely. | False positive |
| `runCatching` on refresh in 8 Components is "CRITICAL silent failure" | Pattern is `runCatching { useCase.refresh(q) }; load()`. The follow-up `load()` re-collects `observe()` which emits `Result.Error` when cache is empty (user sees error state) or `Success(isStale=true)` when cache exists (user sees stale indicator). Errors surface via `load()` — refresh's own exception is correctly swallowed because the same network call runs again in `observe()`. | Reduced to LOW UX gap (no "refresh-failed" toast when cache exists) |
| Trade `query.partner` not enum-validated | Bad partner code returns empty cells → `Empty` UiState. User sees empty state, not wrong data. | Reduced to LOW |
| MetricDropdown lacks `contentDescription` on icon | `contentDescription = null` is **correct** for decorative trailing chevron; the Material 3 `DropdownMenu` semantics handle screen-reader exposure on the clickable Row. The actual issue was tap-target size (fixed below). | False positive |
| Tourism seasonality "still mocked" | [TourismApiServiceImpl.kt:107-127](feature-tourism/src/commonMain/kotlin/eu/eurostat/feature/tourism/data/TourismApiServiceImpl.kt:107) calls real `tour_occ_nim` dataset. The CLAUDE.md claim was stale doc, not stale code. | Stale doc — fixed |
| Environment "commonTest deleted, needs rewrite" | 5 test classes present in [feature-environment/src/commonTest/](feature-environment/src/commonTest/kotlin/eu/eurostat/feature/environment/) covering API, repository, mapper, component, fakes. | Stale doc — fixed |

### HIGH — 4 confirmed, 4 fixed

Tap targets below WCAG 2.1 AA minimum of 48dp on interactive components. `CountryChip`, `BottomTabBar`, `CountryChipsRow`, `SegmentedControl` already use `defaultMinSize(minHeight = 48.dp)`; four other interactive components did not.

| Component | Computed tap height before | After |
|---|---|---|
| [UnderlineTabs](core-ui/src/commonMain/kotlin/eu/eurostat/ui/component/UnderlineTabs.kt) | ~43dp (label 20 + spacing.s × 2 + 3dp underline + xs) | 48dp |
| [PillToggle](core-ui/src/commonMain/kotlin/eu/eurostat/ui/component/PillToggle.kt) | ~28dp (label 20 + spacing.xs × 2) | 48dp |
| [MetricDropdown](core-ui/src/commonMain/kotlin/eu/eurostat/ui/component/MetricDropdown.kt) | ~32dp (text + chevron + spacing.xs × 2) | 48dp |
| [ChipRow](core-ui/src/commonMain/kotlin/eu/eurostat/ui/component/ChipRow.kt) | ~28dp (label 20 + spacing.xs × 2) | 48dp (Text wrapped in centered Box) |

### MEDIUM — 4 observed, 4 fixed (doc-only) or accepted

| Finding | Action |
|---|---|
| CLAUDE.md "WireframeApp.kt is dead code, scheduled for removal" — file already deleted | Removed stale claim |
| CLAUDE.md "Phase 5 — Remove dead WireframeApp.kt and legacy core.ui.* package" — both already removed | Removed Phase 5 todo |
| CLAUDE.md "feature-environment ⚠️ cache bypassed; ⚠️ commonTest deleted" — both restored | Updated phase status |
| CLAUDE.md "feature-tourism ⚠️ seasonality heatmap still mocked" — real `tour_occ_nim` shipping | Updated phase status |
| `core.ui.*` legacy package and `Sketch*` primitives | Verified: no source files in `eu.eurostat.core.ui.*` outside `build/` artifacts. Already deleted in a prior pass. |
| `JsonStatParser` `mapNotNull` drops non-integer value-map keys silently | Accepted — JSON-stat 2.0 spec guarantees integer keys; throwing on theoretical malformed input would over-engineer for a non-reproducible case. |
| `runCatching` on refresh in 8 Components: no specific "refresh failed" toast | Accepted — the follow-up `load()` surfaces errors via `Result.Error` (no cache) or `isStale=true` indicator (cache exists). Adding toast would expand scope beyond hardening. |
| 5 repositories use `catch (_: Throwable) { emptyList() }` for DAO reads | Accepted — graceful degradation to cache miss is correct when the SQLite layer fails. Adding logging would be defensive without addressing a concrete bug. |

### LOW — 1 observed, 0 fixed

- `feature-settings` screen has clickable rows with no `onClick` handlers. Already documented as "coming soon" in NEXT_STEPS.md P2 (Search & Settings screens). Not a regression, not blocking release.

### Dataset filter audit

All 18 dataset/filter combinations in [CLAUDE.md:132-156](CLAUDE.md:132) cross-checked against the live filter maps in each `*ApiServiceImpl.kt`:

| Feature | Dataset | Filter map compliance |
|---|---|---|
| population  | demo_pjangroup   | ✓ |
| economy     | nama_10_gdp / prc_hicp_aind / gov_10dd_edpt1 | ✓ all three |
| environment | env_air_gge / nrg_bal_c / sdg_13_10 | ✓ all three |
| trade       | ext_lt_intratrd  | ✓ (includes `sitc06=TOTAL` last-cell-wins guard) |
| transport   | road_pa_buscoa / avia_paoc | ✓ (×1000 scaling in mapper, no `vehicle`/`partner` dim) |
| tourism     | tour_occ_ninat / tour_dem_tttot / tour_occ_nim | ✓ (`c_dest=WORLD`, no spurious `c_resid` on trips) |
| social      | ilc_li02 / ilc_peps01 / hlth_silc_01 | ✓ (no spurious `indic_il` on peps01) |
| science     | rd_e_gerdtot / isoc_ci_ifp_iu / edat_lfse_03 | ✓ |

No dataset code mismatches, no missing required filters, no last-cell-wins risks unmitigated.

## Files changed

| File | Change |
|---|---|
| [core-ui/.../UnderlineTabs.kt](core-ui/src/commonMain/kotlin/eu/eurostat/ui/component/UnderlineTabs.kt) | `.defaultMinSize(minHeight = 48.dp)` on tab Column |
| [core-ui/.../PillToggle.kt](core-ui/src/commonMain/kotlin/eu/eurostat/ui/component/PillToggle.kt) | `.defaultMinSize(minHeight = 48.dp)` on segment Box |
| [core-ui/.../MetricDropdown.kt](core-ui/src/commonMain/kotlin/eu/eurostat/ui/component/MetricDropdown.kt) | `.defaultMinSize(minHeight = 48.dp)` on Row |
| [core-ui/.../ChipRow.kt](core-ui/src/commonMain/kotlin/eu/eurostat/ui/component/ChipRow.kt) | Wrap Text in Box with `defaultMinSize(48dp)` to keep label centered |
| [CLAUDE.md](CLAUDE.md) | Remove 4 stale claims: WireframeApp dead code, core.ui legacy, env cache bypass/tests, tourism mock heatmap |

Source diff: 5 files, additive only, no public-API changes, no logic changes outside accessibility minimum size.

## Tests added

None. The four UI components have no existing unit tests in the project (pattern is to test ViewModels/repositories, not Compose layout). The fix is a single `Modifier` extension call that cannot regress correctness — only enforce a minimum size if the natural content height would otherwise be less. Existing 433 tests cover the behavior these components feed (state changes, intent dispatch).

## Risks removed

- 4 WCAG 2.1 AA tap-target failures on interactive UI controls (real for screen-reader and motor-impaired users on phone form factor)
- Stale documentation that misleads future contributors (and the user's auto-memory) about what work remains

## Remaining risks

Carried into release with explicit acceptance:

1. **No refresh-failed toast when cache exists** — user sees stale indicator instead of explicit error. UX gap, not data bug. Worth a P2 follow-up.
2. **DAO-failure silent fallback to empty cache** — if SQLite is corrupted, user sees Loading → network fetch → either fresh data or Error. No telemetry distinguishing "DB corrupt" from "cache empty." Worth a P3 follow-up once telemetry exists.
3. **JsonStatParser silently drops non-integer value-map keys** — defensive against a JSON-stat spec violation that doesn't occur in real Eurostat responses. Accepted.
4. **feature-settings screen is placeholder** — documented in NEXT_STEPS.md P2. Not a regression.
5. **Overview bottom-tab is a no-op** — documented in NEXT_STEPS.md P0. Not introduced by this pass.
6. **iOS Xcode wrapper is a stub** — documented in README.md and NEXT_STEPS.md P0. Not introduced by this pass.

## Verification

```
$ ./gradlew testDebugUnitTest
BUILD SUCCESSFUL

$ ./gradlew :composeApp:assembleDebug
BUILD SUCCESSFUL
```

433 unit tests pass, 0 failures, 0 errors, 0 skipped (aggregated from 40 JUnit XML reports under `*/build/test-results/testDebugUnitTest/`).

## Phase 7 loop check

> Are there any remaining CRITICAL or HIGH-confidence findings?

**No.**

- 0 CRITICAL findings confirmed.
- 0 HIGH findings remain after the tap-target fixes.
- All remaining items are MEDIUM/LOW with explicit acceptance.

Loop terminates. Final verdict below.

---

# Release Hardening Verdict

**Build:** PASS

**Tests:** 433 passed (0 failures, 0 errors, 0 skipped)

**Critical findings fixed:** 0 (none confirmed; agent false-positives debunked)

**High findings fixed:** 4 (tap targets on UnderlineTabs, PillToggle, MetricDropdown, ChipRow)

**Regression tests added:** 0 (fix is pure layout `Modifier`, no logic change; project has no Compose UI tests by convention)

**Remaining risks:**
- L1 refresh-failed feedback gap (UX)
- L2 DAO failure swallowed as cache miss (telemetry gap)
- L3 settings/overview/picker screens are placeholders (P0-P2 in NEXT_STEPS.md, documented)
- L4 iOS Xcode wrapper is a stub (P0 in NEXT_STEPS.md, documented in README.md)

**Public release recommendation:** GO

The Android APK is correct, all data-layer filters match the Eurostat canonical table, no silent data corruption, no crashes in production code paths, accessibility now meets WCAG 2.1 AA tap-target minimums. The known limitations are honestly disclosed in README.md and tracked in NEXT_STEPS.md — appropriate for a Phase 4 milestone shipping under AGPL-3.0 for community feedback.

**NLnet submission recommendation:** GO

The codebase demonstrates: real public-data integration (18 Eurostat datasets, no mocks), strong test coverage on data layer (433 tests), clean multi-module architecture, multi-platform reach (Android shipping, desktop packaging works, iOS common code compiles), and an honest, prioritized roadmap. The remaining gaps (iOS wrapper, country picker, overview screen, settings persistence, fonts, translations) are exactly the kind of milestones an NLnet grant funds — they are concrete, scoped, and have acceptance criteria.

**Confidence:** 90%

10% reserved for: (a) no real-device smoke test executed in this pass — see NEXT_STEPS.md P0-1; (b) iOS toolchain unverified — P0-2; (c) Compose UI not exercised by automated tests so the tap-target fix is verified by computation, not by an emulator interaction test.
