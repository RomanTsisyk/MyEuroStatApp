# Milestone breakdown — EU Stats Multiplatform · NLnet submission

Four milestones over four months. The full budget breakdown is kept in the private
NLnet form (not committed to this public repo). At a high level it is mostly
lead-maintainer development, with a small remainder for independent user testing
and Apple-platform consultation.

Each milestone is tracked as a separate GitHub Milestone on the public repo
(`https://github.com/RomanTsisyk/<REPO>/milestones`) with linked issues and a final
checklist closed when the deliverable is signed off.

Funding model:
- **Lead maintainer development** — the majority of the effort: four milestones
  of roughly equal size (~70 hours/milestone) over four months, at a modest
  open-source rate (roughly 60% of the equivalent senior Android-developer
  commercial rate in the Polish market — typical for grant-funded work in
  Central Europe).
- **Recurring user-testing focus groups** — 5–8 participants every 2–4 weeks
  across the grant period (~6–8 sessions total). Conducted informally and in
  person where possible; refreshments and any small remote-participant honoraria
  funded from the project budget. Each session focuses on the most recent
  milestone's deliverable; feedback is filed as public GitHub issues.
- **Apple-platform consultant** — engaged primarily in M1 (iOS Xcode wrapper
  regeneration, signing, TestFlight) and M4 (macOS packaging, notarization
  guidance). Hourly engagement; not a project co-developer.

These contributors and validation activities bring real-user feedback into the
loop and reduce single-point-of-failure risk on platform-specific work, without
inflating the project to a "team" we cannot honestly justify.

---

## M1 — iOS end-to-end verification (Overview already shipped)
**Effort: ~70 hours lead dev + ~5 hours external advisor · GitHub milestone "v0.5 · iOS"**

### Why this first

The "Multiplatform" name in the title needs to be defensible on day one of the
funded period. Android ships, and the entire common codebase now provably
compiles for iOS Native — verified this cycle by a green
`./gradlew :composeApp:compileKotlinIosSimulatorArm64`. What no one has seen yet
is the app *launching* on iOS: the Xcode wrapper is a stub and has never been run
end-to-end. The Overview landing screen — once the other weak spot of this
milestone — has already been built ahead of the grant (see "Already delivered"),
so M1 now concentrates squarely on closing the iOS runtime gap.

### Deliverables

- `xcrun xcodebuild -version` succeeds (currently exits 72; toolchain regen)
- `./gradlew :composeApp:linkDebugTestIosSimulatorArm64` passes (the *compile*
  step, `compileKotlinIosSimulatorArm64`, is already green)
- App launches in iOS simulator, all 8 tabs render real Eurostat data
- TestFlight build distributed to the author's own Apple ID for device verification
- Public `RUN_REPORT.md` with screenshots from Android device + iOS simulator

**Already delivered ahead of the grant** (evidence of execution, not funded hours):

- `feature-overview` module — the data-rich Overview dashboard is now the app's
  landing screen: a hero GDP stat plus a responsive per-module teaser grid that
  shows one live metric per module, each tile degrading independently so a single
  broken dataset never breaks the landing. Unit-tested with 8 fake repositories.

*Post-submission update (July 2026, before review):* the iOS runtime gap this
milestone targets has been largely closed on the author's own time. The stub
Xcode wrapper was replaced with a real project generated from `iosApp/project.yml`
(XcodeGen, manifest checked in), the toolchain root cause was identified
(`xcode-select` pointing at CommandLineTools) and the integration gotchas fixed
(`-lsqlite3`, `CADisableMinimumFrameDurationOnPhone`, Kotlin/Native `doInit`
export naming, JAVA_HOME pinning in the Xcode build phase). The app now builds
with `xcodebuild` and launches on the iPhone 17 simulator rendering live
Eurostat data. Still open from this milestone: physical-device verification,
TestFlight distribution, the public `RUN_REPORT.md` with the 8-tab walk-through,
and green `:composeApp:build` across all targets. At MoU time the author
proposes rescoping the freed hours toward the stretch list at the bottom of
this document.

### Acceptance

- `./gradlew :composeApp:build` passes ALL targets including iOS (the iOS
  *compile* already passes; this milestone adds link + runtime launch)
- The app launches on an iOS simulator and a physical device, all 8 tabs
  rendering real Eurostat data
- Issues in the M1 GitHub milestone are all closed

---

## M2 — Country picker + Comparison mode
**Effort: ~70 hours lead dev + ~5 hours external advisor · GitHub milestone "v0.6 · Picker + Compare"**

### Why

The `+ add` chip on every screen currently does nothing — there is no country
picker. Multi-country comparison is the flagship interaction per the original
design brief but cannot be reached because there is no picker to add the second
country.

### Deliverables

- `feature-picker` (or `core-ui/component/picker/`) — searchable country list as a modal bottom sheet
- 27 EU + 4 EFTA flag SVGs bundled via Compose Resources, replacing the 14×10 dp placeholder rectangles
- `CountryChip` updated to take a country code and resolve to a real flag
- `feature-compare` — multi-country line/small-multiples chart accessible from any feature's `ModuleAppBar`
- "% of base year" normalization toggle for cross-country comparison

### Acceptance

- Tapping `+ add` opens the picker on every feature screen
- Comparison screen overlays 2–3 countries on any of the 8 indicators with distinct module-accent colours

### Out of scope

- Tap-the-map variant of the picker (defer to a future milestone)

*Post-submission update (July 2026, before review):* partially pre-delivered on
the author's own time: a searchable `CountryPickerSheet` now opens from the
`+ add` chip on every feature screen (Unicode-emoji flags for now — the bundled
SVG flag set stays in this milestone), and the economy screen ships the
multi-country overlay with an index-stable series palette plus the
"% of base year" (Indexed 100) normalization toggle, unit-tested. Still open:
SVG flags via Compose Resources, and the dedicated cross-module comparison
screen reachable from every module.

---

## M3 — Tablet & desktop responsive + PL/UK localization
**Effort: ~70 hours lead dev + ~5 hours external advisor · GitHub milestone "v0.7 · Responsive + Locale"**

Two adjacent pieces combined into one month — they touch the same Compose code
and benefit from being shipped together.

### Why

The desktop UberJar already builds — the codebase ships to macOS/Linux/Windows
today. But every screen is phone-first, so a 13-inch window shows a 360 dp
column with empty bands. Tablet is the same: works, but looks wrong.

Localization: every user-facing string is a hardcoded Kotlin literal. PL + UK
are explicitly listed as target locales in `CLAUDE.md`. Eurostat returns EN/DE/FR
labels natively for many dimensions; PL + UK need our translation.

### Deliverables

- `WindowSizeClass` detection at the root of every Screen (Compact / Medium / Expanded)
- ≥ 840 dp → two-pane master-detail layout
- Compose Resources string extraction for all 8 feature modules + core-ui (estimated 200–250 strings)
- PL translation by the author (working knowledge of Polish; small native-speaker review from the budget if needed)
- UK translation by the author (Ukrainian is the author's native language)
- Locale-aware number formatting helpers in `core-ui/format/NumberFormat.kt` (PL `,` decimal + ` ` thousands; UK `,` thousands etc.)

### Acceptance

- All 8 screens render correctly at 360 / 600 / 840 / 1280 dp widths
- Zero hardcoded user-facing strings in feature Screens
- Language picker in Settings (placeholder today) switches PL/UK/EN at runtime

*Post-submission update (July 2026, before review):* the locale-aware number
formatting deliverable listed above shipped early, exactly as scoped:
`core-ui/format/NumberFormat.kt` with per-platform decimal/grouping separators
(expect/actual on Android/desktop/iOS), one rounding convention, and all eight
screens migrated off their inline formatters (29-case test suite). The Settings
screen is also no longer a placeholder — theme/language/default-country persist
in SQLDelight and the theme preference is applied app-wide. The string
extraction, PL/UK translations, responsive layouts and *applying* the language
preference remain the funded body of this milestone.

### Out of scope

- DE + FR app strings (Eurostat returns these natively; system translations sufficient for v0.7)
- IT/ES/PT/NL (defer; these were translated for the landing page only)
- Three-pane desktop layout (defer)

---

## M4 — Native installers + signed Android + CI/CD
**Effort: ~70 hours lead dev + ~5 hours external advisor · GitHub milestone "v1.0 · Public release"**

### Why

The point of a 4-month grant cycle is to end with something a non-developer
citizen can install. Currently the project ships an APK to people who know how
to enable Unknown Sources, and a `.jar` to people who have a JDK installed.
That's not "for citizens."

### Deliverables

- `compose.desktop { nativeDistributions { targetFormats(Dmg, Msi, Deb) } }` configured
- Signed Android release APK + AAB ready for Google Play
- F-Droid metadata submitted to inclusion queue
- Linux `.deb` package + Flatpak manifest (community-submittable to Flathub)
- macOS `.dmg` (unsigned for v1.0; notarization deferred — see Notes)
- Windows `.msi` (unsigned for v1.0 — see Notes)
- GitHub Actions CI: a first workflow already runs `:composeApp:assembleDebug` + `allTests` on every push (green — an early down-payment on this milestone); M4 extends it so tag pushes build and upload the native installers as Release artifacts
- App icon + adaptive Android icon (currently default robot)
- Proguard/R8 rules verified for Decompose, Ktor, kotlinx.serialization
- Landing page download badges link to GitHub Releases page

### Acceptance

- `git tag v1.0.0 && git push --tags` produces all four native artifacts as a GitHub Release
- Android APK installs on a stock Pixel from F-Droid inclusion (after F-Droid build queue)
- Tester on macOS / Linux / Windows can double-click the installer and launch the app

### Notes on signing

- Apple Developer Programme membership and a Windows code-signing certificate are NOT included in this grant. The v1.0 release ships unsigned macOS and Windows binaries (users see Gatekeeper / SmartScreen warnings, can bypass). Signed releases are deferred to a post-grant phase or a stretch goal if earlier milestones finish ahead of schedule.
- This keeps M4 honest: it funds dev time only, no infrastructure / certificate overhead the author can't justify reusing across other projects.

---

## What is NOT in this grant

Items tracked in `NEXT_STEPS.md` but explicitly NOT funded here:

- **Independent WCAG 2.1 AAA accessibility audit** — the external contractor cost is hard to justify at this grant size. The codebase already enforces WCAG 2.1 AA tap-target minimums (48 dp). Self-conducted screen-reader pass can happen post-grant.
- **Apple App Store / Mac App Store / Microsoft Store paid submissions** — annual fees + per-store overhead. Out of scope; v1.0 release ships via GitHub Releases + F-Droid + (community) Flathub.
- **Pull-to-refresh, detail modal on chart tap, chart performance profiling** — P3 items, post-1.0.
- **Federated data sources** (Destatis, INSEE, GUS alongside Eurostat) — Phase 9, future grant.
- **DE/FR/IT/ES/PT/NL app strings** — only PL + UK funded here. The landing page already has all 8 EU translations (community contribution welcome for app strings).

---

## Why this budget size and not more

Three reasons, in order:

1. **Realism.** The maintainer lives in Poland and works on this project as an
   independent contributor with 13+ years of professional Android experience
   (HP Inc Polska, GlobalLogic, BV Group). The requested rate is the honest rate
   for senior Kotlin work on a public good — roughly 60% of the equivalent Polish
   commercial market rate. The small remainder reserved for independent testing
   and Apple-platform consultation reflects actual planned external help; asking
   for more without an agency structure would be padding.

2. **Approval probability.** The ask sits comfortably in the middle of the NGI
   Zero Commons range and lets reviewers say yes without a long internal
   justification. A larger ask from a solo contributor with a 4-month timeline
   gets harder questions; better to ship a small grant cleanly and apply for a
   follow-up than to overpromise.

3. **Scope honesty.** With roughly four months of part-time effort, the four
   milestones above are tight but achievable. Adding a 5th or 6th milestone
   (a11y audit, store submissions, fonts, search screen) would either compress
   the existing work past plausibility or stretch the timeline beyond what a
   solo contributor can credibly sustain alongside other obligations.

If M1–M4 finish under budget or ahead of schedule, the remaining time goes to
P3 items from `NEXT_STEPS.md` — pull-to-refresh, locale-aware error messages,
font bundling, etc. — and the grant report at the end will list these as
stretch deliverables.

If a milestone overruns, the author absorbs the cost (this is how solo OSS
grant work normally goes) and ships the milestone late rather than cutting
quality.
