# Run report — Android emulator smoke test

**Date:** 2026-09-20 · **App:** 0.6.0 debug APK built from `master` (`ebb9f80`) plus the fixes on `fix/android-run-findings`
**Device:** Android Emulator `Medium_Phone_API_36.1` (API 36, arm64, 1080×2400)
**Locales exercised:** English, Polish, Ukrainian

This is the interactive walk-through NEXT_STEPS.md listed as the last open item
for the Android half of "100% working". It was done in two passes: the first
walked all modules and found the layout/data defects; the second covered
offline, rotation, refresh, Search, Compare, Settings and wide screens. It
covers a single emulator only — not a physical device — see
[Not covered](#not-covered).

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
| 9 | every screen, offline | error said "Something went wrong" instead of "No connection" | a really offline device throws `UnknownHostException` / `SocketException`, but only Ktor timeouts were mapped to `NoNetwork` | JVM connectivity exceptions map to `NoNetwork` (Android + desktop); iOS `NSURLErrorDomain` codes too (`UrlErrorClassification.kt`, JVM-tested; the iOS glue is unverified on iOS) |
| 10 | every screen, dark theme | status-bar clock and icons invisible (dark on dark) | icons followed the *system* theme, not the theme picked in Settings | `StatusBarIcons(light)` applied from `EurostatTheme` |
| 11 | Overview | dark header with dark status-bar icons | same | light icons over the Overview header |
| 12 | Overview | `4,387` / `83.5M` in Polish after switching language, until restart | teaser strings formatted once in the component and kept in a singleton | raw values in state, formatted while composing |
| 13 | all module headers | "Germany · DE" in Polish/Ukrainian | English-only country names | 34 localized `country_*` strings, `countryDisplayName()` |
| 14 | Trade | the headline number was always the balance, while the subtitle under it named the selected tab (Exports / Imports) | value hard-wired to the balance | `tradeHeadlineValue` follows the selected tab (`TradeFormattingTest`) |
| 15 | Science | the three sparkline tiles showed the latest year's value under a headline for an earlier selected year | series always ended at the latest year | `toSparkSeries(..., upToYear)` — tiles read "as of the selected year" (`SparkSeriesTest`) |
| 16 | Population, Economy, Environment, Social, Science, Compare | header search icon did nothing (`onSearch = {}`), though Search existed and the Overview header opens it | icon handler never wired | optional `onSearch` on each screen, wired in `EurostatApp` to push Search (Back returns to the module; `RootComponentTest`); the app bar's Back / Search / Refresh / More descriptions are now localized (`ui_action_*`, EN/PL/UK) |

## Found, not fixed

- **iOS offline mapping is written but unproven on a device**: Darwin
  reports `NSURLErrorDomain` codes (e.g. -1009); they are classified from the
  error text by a JVM-tested function (`UrlErrorClassification.kt`), but the iOS
  glue (`ErrorMapping.ios.kt`) was never compiled or run locally — the CI iOS
  job is its first build.
- **Manual refresh while offline with a cache**: the data stays and the
  footer still says "fresh" (the cache is inside its 12 h TTL and a failed
  refresh is swallowed). Accepted earlier as a UX gap; no "refresh failed" hint.
- **Overview offline with an empty cache** shows `—` everywhere with no
  offline hint (the module screens do show the error state).
- **Wide screens**: a phone in landscape and a ~1070 dp-wide tablet both switch
  to the two-pane layout and scroll correctly, but on the tablet the lower
  half of the screen is empty, and in landscape the right pane's viewport is
  short (the chart needs a scroll to be seen whole).
- Country picker list keeps English alphabetical order in PL/UK; the
  Settings default-country label and the Compare legend still show the
  English name / code.
- Science tile label "Wykształcenie wyższe" is ellipsised in Polish.
- Trade, Transport and Tourism show no header search icon (Search is still
  reachable from the Overview header).
- The app bar's "More" pill still has an empty `onClick`. It is drawn only when
  a screen passes no `onRefresh`, which no current screen does, so it is not
  visible today.
- No axis labels on Transport small multiples, Social lines, Tourism bars or
  the seasonality heatmap (months/years) — not investigated whether by design.

## Also covered in the second pass

| Scenario | Result |
|---|---|
| Airplane mode with a warm cache (Overview + Economy) | cached data shown, no crash |
| Airplane mode, no cache | Overview `—`, Economy error "No connection — check your network" (after fix #9); **retry** after reconnecting loads the data |
| Forced refresh while offline (cache present) | data stays; see "Found, not fixed" |
| Rotation portrait → landscape → portrait on Economy | selected metric (Inflation) survives; landscape uses the two-pane layout and scrolls |
| Search ("GDP") | opens with a browse list; results for GDP, deficit and R&D |
| Compare | GDP for DE / FR / PL loads with the indicator picker |
| Settings: Dark theme, force-stop, relaunch | theme persists and is applied on start |
| Settings: language switch without restart | UI, numbers and country names follow (after fixes #12, #13) |
| ~1070 dp-wide screen (`wm size 1600x2560`, 240 dpi) | two-pane layout |

## Not covered

- **iOS**: `xcode-select` on this Mac points at the Command Line Tools
  (`/Applications/Xcode.app` is present but not selected — see the
  `DEVELOPER_DIR` note in `iosApp/README.md`), so the simulator run and
  `iosSimulatorArm64Test` were not executed in this session
- A physical Android device, and the `Pixel_Tablet` AVD (the wide-screen check
  used `wm size` on the phone emulator instead)
- Process death / state restore, the pull-to-refresh gesture, clearing the cache
  and changing the default country in Settings, the country picker itself
- Other API levels and slow or flaky (as opposed to absent) networks

## Tests

Unit tests were run for every touched module (`testDebugUnitTest`, plus
`desktopTest` for `core-ui` and `core-network`) and pass. New or extended tests:
`YearAxisTest`, `RadarLayoutTest`, `TradeFormattingTest`, `TransportFormattingTest`,
`TransportApiServiceImplTest` (`schedule=TOTAL`), `TransportComponentTest` (default
year), `TeaserFormattingTest` and `OverviewComponentTest` (raw teaser values),
`CountryNameResTest` and `CountryStringsParityTest` (every country has a
localized name), `AndroidNetworkErrorMappingTest` / `DesktopNetworkErrorMappingTest` /
`UrlErrorClassificationTest`, `SparkSeriesTest` (Science) and a
Population → Search → Back case in `RootComponentTest`. The iOS Native test
suite has not been run locally.
