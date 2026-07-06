# NLnet propose form — field-by-field answers

Copy-paste each section into the matching field at https://nlnet.nl/propose/

Placeholders to replace: `RomanTsisyk`, `<REPO>`, `<COUNTRY>`, `<PHONE>` — wherever you see them.

---

## SECTION 1 — Contact information

### Field 1 · Your name
```
Roman Tsisyk
```

### Field 2 · Email address
```
ROMAN.TSISYK1@gmail.com
```

### Field 3 · Phone number
```
+48 728 282 026
```

### Field 4 · Organisation
```
Independent contractor (B2B, Polish tax residence — Wrocław, Poland)
```

### Field 5 · Country
```
Poland
```

---

## SECTION 2 — Thematic call

### Field 6 · Please select a call
```
NGI Zero Commons Fund
```

---

## SECTION 3 — General project information

### Field 7 · Proposal name
```
EU Stats Multiplatform — civic open-data viewer for the public Eurostat API
```

### Field 8 · Website / wiki
```
https://romantsisyk.github.io/<REPO>/
```
(Backup: https://github.com/RomanTsisyk/<REPO>)
(Maintainer profile + other OSS projects: https://github.com/RomanTsisyk · https://roman-tsisyk.com)

### Field 9 · Abstract

```
EU Stats Multiplatform is an independent third-party open-source mobile and desktop client for the public Eurostat dissemination API. Eight thematic modules — economy, population, environment, trade, transport, tourism, social, science — turn the dense JSON-stat 2.0 responses Eurostat publishes into calm, readable charts: pyramids, line series, stacked bars, diverging bars, radar charts, heatmaps, and small multiples. A single Kotlin Multiplatform codebase ships to Android, iOS, macOS, Linux, and Windows; the same Compose UI renders identically on every form factor. Citizens, journalists, students, and researchers can explore the same European public statistical data on whatever device they have — phone in the kitchen, tablet on the train, desktop in the newsroom — with no analytics, no tracking, no cookies, and no vendor lock-in.

The project is licensed AGPL-3.0-or-later. Eurostat data remains the property of the European Union and is republished under CC-BY 4.0 as Eurostat publishes it. The app is not affiliated with, endorsed by, or sponsored by Eurostat or the European Commission.

Current state: all 8 feature modules ship real Eurostat data with no mocks. The data-rich Overview dashboard — the app's landing screen, and an M1 deliverable — is already built and unit-tested, delivered ahead of the funded period as concrete evidence of execution. 499 unit tests pass with zero failures. The entire Kotlin Multiplatform common codebase now provably compiles for every iOS Native target (verified via `compileKotlinIosSimulatorArm64`), which materially de-risks the iOS milestone; the Xcode wrapper and on-device runtime remain the funded M1 work. A GitHub Actions CI pipeline runs on every push. Android APK builds at ~20 MB; desktop UberJar at ~99 MB. The public landing page is live with 9 animated GIF demos and translations in 8 EU languages. The codebase already exists, has been hardened across QA passes, and is published on GitHub.

The grant would fund four focused milestones over four months to reach v1.0: (M1) iOS end-to-end verification (the Overview dashboard is already shipped, so M1 concentrates on the Xcode wrapper, simulator/device runtime and TestFlight), (M2) country picker + comparison mode, (M3) tablet/desktop responsive layouts + PL/UK app-string localization, (M4) native installers + signed Android release + CI/CD pipeline (a first CI workflow already runs). Each milestone is tracked publicly as a GitHub Milestone with linked issues. Development is performed by a single lead maintainer (Polish-based independent contractor, 13+ years professional experience, 8+ years Android specialisation). Quality assurance combines automated tests, recurring focus-group sessions with 5–8 community participants every 2–4 weeks, and an Apple-platform consultant for iOS/macOS verification and packaging. This converts a "ships when it ships" hobby into a clearly-scoped four-month effort with a concrete public-release endpoint.
```

### Field 10 · Prior involvement

```
EU Stats Multiplatform is led by a single maintainer with 13+ years of professional software-engineering experience and 8+ years specialised in Android development. Roman Tsisyk is a Polish citizen working from Wrocław as an independent contractor, currently serving as Senior Android Developer at BV Group (UK, remote). Prior senior roles include nearly three years at HP Inc Polska (Jan 2022 – Nov 2024), where the multi-modular Compose + Kotlin architectural patterns now used in this project were honed across a production BT/IoT app shipped to Google Play with compliance, A/B testing, and an external security audit. Earlier engineering positions at GlobalLogic Poland (Hitachi Group), Aplitt, and GSM Billing Limited cover Bluetooth integration, Flutter cross-platform development, and freelance Android delivery.

The maintainer's public open-source portfolio at https://github.com/RomanTsisyk demonstrates the same engineering discipline applied to standalone Android libraries:

- CryptoKit (MIT, v1.0.0 stable) — production-grade Android cryptography library built on Android Keystore. Provides AES-256-GCM symmetric encryption, RSA-OAEP asymmetric operations, biometric and device-credential authentication integration, JWT creation/validation with algorithm pinning, and a sealed exception hierarchy for exhaustive error handling. 100% Kotlin. https://github.com/RomanTsisyk/CryptoKit

- bleX (MIT, v0.9.0-beta) — production-grade Android BLE abstraction. Kotlin Flow API for reactive state observation, lifecycle-aware auto-disconnect, smart reconnection with exponential backoff, type-safe error handling via a BleResult wrapper, service caching, automatic MTU chunking for long writes. https://github.com/RomanTsisyk/bleX

- NFC-Probe (MIT, active, 8 stars) — Android engineering toolkit for analyzing contactless EMV payment cards at the APDU protocol layer. Implements the full EMV transaction flow (PPSE selection → application selection → GET PROCESSING OPTIONS → READ RECORD), decodes 30+ EMV tags from raw BER-TLV responses. PAN masking, sensitive-data filtering, no network permissions — privacy-by-design. Built with Jetpack Compose, Hilt, Room, GitHub Actions CI/CD. https://github.com/RomanTsisyk/NFC-Probe

The technical stack used in this grant project — Kotlin, Jetpack Compose, multi-modular Clean Architecture, MVVM/MVI, Coroutines/Flow, JUnit + Turbine — is what the maintainer ships every workday in commercial Android work and across these public OSS libraries. The consistent "production-grade" framing, MIT licensing for maximum reuse, sealed-type error modelling, and active CI/CD pipelines on each library demonstrate the same OSS discipline applied at scale across multiple projects.

During the grant period, the maintainer will run recurring user-testing sessions: small focus groups of 5–8 participants every 2–4 weeks, conducted informally and in person where possible, to gather feedback on usability, accessibility, navigation, and data presentation. Refreshments for participants are provided from the project budget. Feedback collected at each session feeds directly into the next milestone's GitHub issues. In parallel, continuous community feedback is gathered through public GitHub issues, discussions, and release-channel reports — anyone using the app can file bugs, suggest improvements, or validate functionality before the next release. Platform-specific work is supported by an Apple-platform consultant assisting with iOS Xcode wrapper regeneration, macOS packaging, and release preparation. None of these contributors are project co-developers; the codebase remains under a single maintainer's responsibility.

This is the maintainer's first NLnet application.
```

---

## SECTION 4 — Requested support

### Field 11 · Requested Amount

_(The requested amount and full budget breakdown are provided directly in the private NLnet propose form and are intentionally kept out of this public repository.)_ The ask sits comfortably within the NGI Zero Commons range and is sized for one solo Polish-based contributor working four months part-time on a clearly-scoped roadmap.

### Field 12 · Budget usage

The budget covers, in order of size:

- **Lead-maintainer development and project maintenance** — the large majority of the grant. Four milestones of roughly equal effort (~70 hours each) over four calendar months, at a modest open-source rate (roughly 60% of the equivalent Polish commercial senior-developer rate). Invoiced per milestone against acceptance.
- **User-testing sessions + Apple-platform consultation** — a small remainder. Recurring focus groups of 5–8 participants every 2–4 weeks (feedback filed as public GitHub issues that feed the next milestone), plus Apple-platform consultation in M1 (iOS Xcode wrapper, TestFlight) and M4 (macOS packaging, notarization).

None of these line items represent project co-developers — the codebase remains under a single maintainer's responsibility. Each milestone is tracked as a public GitHub Milestone with linked issues; acceptance is a closed milestone with a public release-notes entry, no opaque deliverables.

Other funding: none. The project has been bootstrapped on the maintainer's personal time since early 2026. NLnet would be the first external support and would convert a part-time evening project into a four-month focused effort to reach v1.0.

Excluded from the budget (honest scope decisions):
- External accessibility audit — deferred; self-conducted screen-reader passes will happen post-grant. The codebase already enforces WCAG 2.1 AA tap-target minimums (48 dp).
- Apple Developer Programme membership and a Windows code-signing certificate. The v1.0 release ships unsigned macOS and Windows binaries; signed releases are deferred to a follow-up phase.
- All other infrastructure is free: GitHub Actions CI on the open-source tier; GitHub Pages for hosting; F-Droid submission is free.

If milestones finish ahead of schedule, the remaining time goes to P3 items from NEXT_STEPS.md (pull-to-refresh, locale-aware error messages, font bundling), listed in the grant report as stretch deliverables. If a milestone overruns, the maintainer absorbs the cost and ships late rather than cutting quality.

### Field 13 · Comparison

```
The closest historical effort is "Eurostat Mobile" — the European Statistical Office's own app, discontinued in 2019. It was iOS-only, closed-source, and is no longer available on the App Store. Our project covers Android + iOS + desktop and is AGPL — no single-vendor risk and no possibility of being silently removed from a store catalogue.

The official Eurostat website (https://ec.europa.eu/eurostat) provides browser dashboards that are desktop-first, slow on mobile networks, do not work offline, and do not support multi-country comparison overlays as a first-class interaction.

OurWorldInData (https://ourworldindata.org) does brilliant editorial work but is limited to curated indicators — it does not expose the full Eurostat tree of 18+ dissemination datasets across all dimensions.

Statistical office apps from individual member states (Destatis, INSEE, GUS, ISTAT) exist but vary in quality, are siloed per country, and don't enable cross-country comparison.

For analysts, Python's PyJStat and R's rjstat parse JSON-stat into data frames — powerful, but useless for non-coders and not a UX product.

The empty quadrant our project addresses: native mobile-first + free + open-source + EU-data-first + multi-form-factor + privacy by default + comparison overlays. No project we are aware of currently occupies that space.
```

### Field 14 · Technical challenges

```
Four concrete technical challenges across the funded milestones:

1) iOS toolchain verification (M1). The Kotlin Multiplatform common codebase now compiles cleanly for every iOS Native target — verified this cycle by a green `./gradlew :composeApp:compileKotlinIosSimulatorArm64` after fixing a JVM-only stdlib call (`toSortedSet`) that had silently blocked Native compilation. What remains unverified is the Xcode wrapper project (a stub today), full simulator + device runtime launch, signing config, and TestFlight submission. Risk: common code that compiles for iOS may still hit Darwin-specific issues at runtime (memory model, threading, platform `expect`s). Mitigation: a dedicated iOS verification milestone with explicit pass criteria — with the compile step already behind us, the milestone starts from a known-good baseline. Fallback: if Darwin runtime issues require deeper work, M1 still lands the signing/packaging groundwork and iOS device launch defers to a follow-up (the Overview screen this milestone was once paired with is already shipped).

2) Country picker + comparison overlay (M2). The picker is currently a placeholder div on every screen. Building it touches: searchable list UI, 31 country flag assets (27 EU + 4 EFTA) as Compose Resources, the `+ add` chip on 8 different feature screens, and a new comparison screen that renders multiple ChartSeries on the same chart canvas. Risk: cross-country comparison may surface scale-mismatch issues that need a normalization toggle (% of base year). Mitigation: ship the searchable picker first, add the comparison overlay only after picker is stable.

3) Responsive Compose Multiplatform layouts (M3). The app is phone-first today. On a 13-inch desktop window a 360 dp column sits centred with empty bands. Funded work: BoxWithConstraints / WindowSizeClass detection at the root of each Screen; ≥ 840 dp → two-pane master-detail. The desktop infrastructure ships already (the UberJar packages and runs); the pure-Compose layout work has not. Risk: some chart types (radar, pyramid, heatmap) may not scale well into wide aspect ratios without redesign. Mitigation: pin charts to a max width in the right-pane and let the surrounding cards absorb the extra space.

4) Localized string extraction + locale formats (M3, combined). Every user-facing string is a hardcoded Kotlin literal inline. Migration to Compose Resources, then translation of EN → PL + UK, with locale-aware number formatting (PL uses "," decimal + " " thousands; UK uses "," thousands; etc.). Risk: Compose Resources has known platform-specific quirks across Android/iOS/desktop. Mitigation: prototype on a single feature module first, then roll out.

The data layer is already solved: a generic JSON-stat 2.0 parser handles all 19 Eurostat datasets with no per-dataset code; per-feature CellMapper extensions shape the cells into typed domain models. Stale-while-revalidate caching is implemented and tested per module. This layer is actively maintained against a live, changing upstream — a recent audit re-verified all 19 dataset codes against the production Eurostat API and caught two that Eurostat had restructured (`ilc_li02` lost a dimension, so its old filter now returns HTTP 400; `ilc_peps01` froze at 2020), both fixed the same day by migrating to the current dataset codes. The funded milestones do NOT change the data-layer architecture — they're pure UI + distribution work on top of an already-tested, actively-maintained foundation.
```

### Field 15 · Ecosystem

```
The project sits at the intersection of three communities that we want to actively engage:

Open data + civic-tech. Publishing official European statistics in a calm, mobile-friendly client serves journalists writing about EU policy, students learning data literacy, policy researchers at NGOs without enterprise tooling, and citizens curious about Europe-wide trends in their own kitchen language. We plan to engage these communities via: blog posts on the NGI Zero diary, conference talks at EU Data Forum and openSUSE-style events, and partnerships with Wikimedia chapters for embedding data in articles.

Kotlin Multiplatform + Compose Multiplatform. The KMP + Compose Multiplatform stack is European technology (JetBrains is based in the Netherlands and Czech Republic). Our project demonstrates an under-explored use case for KMP — civic-tech rather than the typical fintech/social demos. We plan to: present our architecture at KotlinConf and Droidcon, publish a case study, contribute upstream when we encounter library bugs (already opened issues against Decompose and SQLDelight during development), and maintain the project as a reference implementation for KMP newcomers.

Accessibility + design. The codebase already enforces WCAG 2.1 AA tap-target minimums (48 dp) across all interactive controls — verified in two release-hardening QA passes. Independent a11y audit is not funded in this grant (honest scope decision); a self-conducted screen-reader pass is planned for the post-grant period and findings will be filed publicly on the GitHub issue tracker for community contribution.

Engagement strategy across all three: public issue tracker on GitHub from day one, AGPL license to maximise re-use, monthly release cadence post-grant, public roadmap, and a dedicated email channel for journalists and educators who want to integrate the app into their work.

User validation is built into the development loop. Throughout the four-month grant period, the maintainer will run recurring focus-group sessions every 2–4 weeks with 5–8 community participants — students, journalists, NGO researchers, and curious citizens from the Wrocław area and the wider EU open-data network. Sessions are informal, in person where possible (with refreshments provided from the project budget), and remote otherwise. Each session focuses on what shipped in the most recent milestone: usability, accessibility, navigation, chart legibility, and translation quality. Feedback is filed as public GitHub issues and feeds directly into the next milestone's planning. In addition, continuous feedback is gathered through public GitHub issues, discussions, and release-channel reports — keeping the validation loop open between sessions and beyond the grant.
```

---

## SECTION 5 — Attachments

### Field 16 · Project attachments (optional, 50 MB max, HTML/PDF/ODF/text only)

Recommended attachments (from `attachments/` folder):

1. `architecture.html` — single-file architecture reference with inline SVG diagrams (a self-contained snapshot of `/docs/technical/index.html`).
2. `landing-snapshot.html` — snapshot of the landing page.
3. `03-milestones.md` (rename to `.txt` if `.md` not accepted) — detailed milestone breakdown.
4. `02-genai-disclosure.md` (rename to `.txt`) — full GenAI provenance log.

If file uploads fail, all of these are publicly visible at the live site. Refer to them via URL in field 9 (Abstract) or field 12 (Budget).

---

## SECTION 6 — GenAI disclosure (required)

### Field 17 · GenAI use
```
I have used generative AI
```
(NLnet policy requires disclosure when any generative-AI tool helped draft, translate, or summarise application content. See `02-genai-disclosure.md` for full details.)

### Field 18 · AI model details

Paste the entire content of `02-genai-disclosure.md` here (see that file for the verbatim text).

### Field 19 · AI prompt files (optional file upload)

Attach `02-genai-disclosure.md` as a file in addition to pasting it in field 18 — NLnet's policy says applicants should both disclose in form fields AND submit prompt logs as files where possible.

---

## SECTION 7 — Privacy & submission

### Field 20 · Privacy acknowledgment
- [ ] I have read and understood NLnet's Privacy Statement (https://nlnet.nl/privacy)

### Field 21 · Email copy
- [x] Send me a copy of this application

### Field 22 · PGP pubkey (optional)
```
(leave blank unless you have a PGP key and want encrypted reviewer comms)
```

---

## After submitting

You should receive an automated acknowledgement within 24 hours. Save the application reference number — NLnet will use it in all future correspondence. The NGI Zero Commons Fund panel will review applications received before 1 June 2026 12:00 CEST; decision letters typically arrive within 6–8 weeks of the deadline.
