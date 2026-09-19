# Run report — Android emulator smoke test

**Date:** 2026-09-20 · **App:** 0.6.0 debug APK built from `master` (`ebb9f80`) plus the fixes on `fix/android-run-findings`
**Device:** Android Emulator `Medium_Phone_API_36.1` (API 36, arm64, 1080×2400)
**Locales exercised:** English, Polish, Ukrainian

This is the interactive walk-through NEXT_STEPS.md listed as the last open item
for the Android half of "100% working". It covers a single emulator only —
not a physical device — see [Not covered](#not-covered).

## Method

Fresh install (`pm clear` + `adb install`), then for every module: tap its
tile on Overview, wait 9 s for the first fetch, screenshot, press Back. After
the walk the crash log buffer (`logcat -b crash`) was read. The whole walk was
repeated in Polish and Ukrainian (language switched from Settings).

## Result

All 8 modules open, fetch **live Eurostat data** and render their hero chart
with no crash, ANR or empty state. Values seen (Germany unless noted):

| Module | Headline | Chart |
|---|---|---|
| Population | 83.5 M (2024) | 18-cohort pyramid |
| Economy | 4,387 B € GDP | GDP lines DE / EU27 / FR / PL, Absolute ↔ Indexed 100 |
| Environment | 650 Mt CO₂-eq | GHG lines, energy 177,745 ktoe, SDG-13 51.9 |
| Trade | −14 B € balance (exports 840, imports 854) | diverging bars 2017–2024 |
| Transport | road 5.2 bn (2023), air 185 M | road + air small multiples |
| Tourism | 84.8 M foreign nights | stacked bars + seasonality heatmap |
| Social (EU27) | 16.2 % at risk of poverty | 3-series highlight line |
| Science (EU27) | 2.24 % R&D of GDP | radar + 3 sparklines |

`logcat -b crash` was empty after every walk (EN, PL, UK).

## Defects found and fixed

Screens before the fix are in [`docs/run-report/before/`](docs/run-report/before/).

| # | Screen | Symptom | Cause | Fix |
|---|---|---|---|---|
| 1 | Transport | AIR tile `—`, AIR line missing, for every country | `avia_paoc` was queried with `schedule=TOT`; the Eurostat code is `TOTAL`. The API silently returns zero rows for unknown codes | `schedule=TOTAL` + regression tests. `CLAUDE.md` claimed this filter was verified — it was not |
| 2 | Transport | after #1, opened on 2024 with headline `—` | default year was the last year of road ∪ air; road ends a year earlier | default = latest year with road data |
| 3 | Economy, Environment, Compare | X axis `2K … 2K` | year values went through the compact K/M/B formatter | `yearAxis()` in `core-charts` |
| 4 | Trade | blank gap, lone "Exports" label | `UnderlineTabs` underline used `fillMaxWidth()` in a wrap-content column, so the first tab took the whole row and the others collapsed to zero width | `IntrinsicSize.Max` on the tab column |
| 5 | Environment | `177745 ktoe` | `formatDecimal` does not group | `formatGrouped` (GHG too) |
| 6 | Overview vs Economy / Trade | 4,387 vs 4,386; 840 vs 839 | Overview rounded, Economy/Trade truncated | all three round; Trade uses `−` |
| 7 | Social, Science, Transport | units broke mid-word (`G/DP`, `ilc_li/02`, `200/M`), uneven tile heights | narrow side-by-side caption; normal space in value | stacked caption, equal-height tiles, no-break space |
| 8 | Science | radar without axis labels; legend showed `EU27_2020` | labels were passed but never drawn | `showAxisLabels`, legend uses country names |

## Found, not fixed

- **Overview numbers keep the old locale after a runtime language switch**
  (`4,387` / `83.5M` in Polish until the app is restarted). Cold start is
  correct (`4 387`, `83,5M`) — the teaser strings are formatted once when data
  arrives, not per composition.
- **Overview status-bar icons are dark on the dark header** (clock barely
  visible) — the header does not set light status-bar appearance.
- **Country names are English in every locale** ("Germany · DE" in PL/UK, radar
  legend) — the name table is English-only.
- Science tile label "Wykształcenie wyższe" is ellipsised in Polish.
- No axis labels on Transport small multiples, Social lines, Tourism bars or
  the seasonality heatmap (months/years) — not investigated whether by design.

## Not covered

- Offline / airplane mode, recovery on reconnect, refresh and pull-to-refresh
- Rotation and process-death state restore
- Search, Compare and Settings persistence (only the Settings language picker was used)
- Tablet / desktop layouts (`Pixel_Tablet` AVD and the desktop app)
- **iOS**: this Mac has no full Xcode install (only Command Line Tools), so the
  simulator run and `iosSimulatorArm64Test` could not be executed
- A physical Android device

## Tests

Module unit tests were run for every touched module. New or extended tests:
`YearAxisTest` (4), `RadarLayoutTest` (4), `TradeFormattingTest` (7),
`TransportFormattingTest` (4), `TransportApiServiceImplTest` (14, three new
covering `schedule=TOTAL`), `TransportComponentTest` (18, two new covering the
default year).
