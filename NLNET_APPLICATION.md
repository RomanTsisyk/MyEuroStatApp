# NLnet application — EU Stats Multiplatform

> ⚠️ **Superseded draft — do not submit from this file.** This is an early v0.4
> draft scoped at **€25,000 / 6 milestones**. The canonical, current submission —
> **€16,000 / 4 milestones**, with updated facts (499 tests, Overview dashboard
> shipped, iOS common code verified to compile, CI live) — is in
> **`NLNET_SUBMISSION/`** (`01-form-fields.md`, `02-genai-disclosure.md`,
> `03-milestones.md`). Kept only for history.
>
> ℹ️ §4 has been corrected: the earlier MessagePack-Java / Tarantool
> prior-involvement claims were a namesake mix-up (a different, well-known
> open-source developer who shares the name "Roman Tsisyk") and have been
> replaced with the author's actual Android background, matching the canonical
> submission.

**Status:** draft v0.4 · 26 May 2026 · SUPERSEDED (see banner above)
**Fund:** NGI Zero Commons Fund (recommended)
**Apply at:** https://nlnet.nl/propose/

This document is a copy-paste-ready answer set for the NLnet propose form. Each numbered section maps directly to a form field.

---

## 1. Project name

**EU Stats Multiplatform**

## 2. Website / wiki

`https://romantsisyk.github.io/MyEuroStatApp/` (live landing)
`https://github.com/romantsisyk/MyEuroStatApp` (source)

## 3. Abstract (≤1200 chars)

EU Stats Multiplatform is an independent third-party open-source mobile and desktop client for the public Eurostat dissemination API. Eight thematic modules — economy, population, environment, trade, transport, tourism, social, science — visualize official European statistical data through a single Kotlin Multiplatform codebase that ships to Android, iOS, macOS, Linux, and Windows. The app turns the dense, multi-dimensional JSON-stat 2.0 responses Eurostat publishes into calm, readable charts: pyramids, line series, stacked bars, diverging bars, radar charts, heatmaps, and small multiples — all rendered on pure Compose Canvas with no third-party chart dependency. Citizens, journalists, students, and researchers can explore the same European public data on whatever device they have, with no analytics, no tracking, no cookies, and no vendor lock-in. The project is licensed AGPL-3.0; Eurostat data is published by the European Union under CC-BY 4.0.

## 4. Have you been involved with projects or organisations relevant to this project before?

The author is a Senior Android Developer with 13+ years of professional software experience and 8+ years specialised in Android — currently at BV Group (UK, remote) and previously at HP Inc Polska (Jan 2022 – Nov 2024), GlobalLogic Poland (Hitachi Group), and Aplitt. Public open-source work includes **CryptoKit** (MIT — Android Keystore cryptography), **bleX** (MIT — a coroutine-based Bluetooth Low Energy library), and **NFC-Probe** (MIT — a privacy-by-design EMV/NFC toolkit), all at https://github.com/RomanTsisyk. EU Stats Multiplatform applies the same engineering discipline to civic-tech: contract-first APIs, sealed `Result` types, full test coverage, and an editorial UI standard. The author is a Polish citizen based in Wrocław and produces public-interest code as a default.

*(Disambiguation: a different, well-known open-source developer of the same name — Roman Tsisyk of the Tarantool / MessagePack projects — is not the author of this project. This project's author is the Android developer described above; see https://github.com/RomanTsisyk and roman-tsisyk.com.)*

## 5. Requested support

**€25,000 over 6 milestones, 6 months.**

| # | Milestone | Deliverable | Funding |
|---|---|---|---|
| M1 | **Verified iOS + Overview launchpad** | iOS Xcode wrapper rebuilt, app verified on Android device + iOS simulator; Overview dashboard screen wired (currently a no-op). | €4,000 |
| M2 | **Country picker + Comparison mode** | Searchable country picker (currently a placeholder); multi-country chart overlay (compare 2–3 countries on the same indicator). Both are flagship features per the design brief. | €5,000 |
| M3 | **Tablet + desktop responsive layouts** | Master-detail layouts for widths ≥ 840 dp (tablet landscape) and ≥ 1200 dp (desktop). Three form factors, one codebase — already builds, needs UI polish. | €4,000 |
| M4 | **Localization: 5 EU languages + locale formats** | Compose Resources string extraction; PL + UK app strings (Eurostat returns EN/DE/FR natively); locale-aware number formatting (€, decimals, thousand separators per locale). | €5,000 |
| M5 | **Native installers + signed Android release** | macOS `.dmg`, Windows `.msi`, Linux `.deb` + Flatpak. Android signing keys + Play Store / F-Droid submission. CI/CD pipeline on GitHub Actions. | €4,000 |
| M6 | **Accessibility audit + WCAG 2.1 AAA** | Independent a11y audit; screen reader full coverage; tap targets, contrast, focus order, keyboard navigation across all 8 modules. Public audit report. | €3,000 |

Funds cover author time (€60/hour blended rate · independent contractor in EU jurisdiction · invoiced quarterly against milestone acceptance). No agency overhead, no infrastructure costs beyond signing certificates (~€500 included in M5).

## 6. Compare your own project with existing or historical efforts

| Project | What it does | Why this is different |
|---|---|---|
| **Eurostat website** | Browser-based dashboards | Desktop-first, browser-only, slow on mobile, no offline support, no comparison overlays. We're a native mobile-first app with cache. |
| **OurWorldInData** | Curated chart embeds | Brilliant editorial work but limited to selected indicators; doesn't expose the raw multi-dimensional Eurostat tree. We expose the API directly. |
| **Eurostat Mobile** *(EU's own app)* | Discontinued in 2019 | Was iOS-only, closed-source, no longer maintained. We're cross-platform, open-source, AGPL — no single-vendor risk. |
| **Pandas / R / Stata users** | Roll their own | Requires programming skills. We serve non-coders: journalists on deadline, students on a tablet. |

No existing project ships official-API civic-data visualization to mobile + tablet + desktop from a single codebase, with an Apple-Health-quality calm UI, under a strong copyleft license.

## 7. Technical description

**Stack:**
- Kotlin Multiplatform (JetBrains' EU-led project)
- Compose Multiplatform for UI (single source-of-truth across all platforms)
- Decompose for cross-platform navigation
- Ktor for HTTP; kotlinx.serialization for JSON-stat 2.0 parsing
- SQLDelight for the local cache
- Koin for dependency injection
- AGPL-3.0-or-later

**Architecture:**
- 18 Gradle modules, layered Clean Architecture
- Per-feature `data` → `domain` → `ui` strict separation
- `Result<T>` sealed-type contract; no exceptions cross the data/domain boundary
- Stale-while-revalidate caching (12-hour TTL, offline survival)
- Custom JSON-stat 2.0 parser (sub-millisecond on device for typical Eurostat payloads)
- Pure Compose Canvas charts (no third-party chart library) — 8 chart types in ~1,500 LOC
- 499 unit tests, 0 failures, 0 skipped
- WCAG 2.1 AA tap-target compliance (48dp minimum across all interactive controls)

**Privacy by design:**
- No analytics. No telemetry. No cookies. No tracking.
- App talks only to the official `ec.europa.eu/eurostat/api/dissemination` endpoint.
- All caches local-only (SQLite via SQLDelight). No third-party servers.
- Web landing: no JS frameworks, no CDN scripts beyond Google Fonts for Inter (can be self-hosted in M5).

**Current state:**
- ✅ Phase 4 complete: 8 modules end-to-end with real Eurostat data
- ✅ Android APK builds (~20 MB)
- ✅ Desktop UberJar builds (~99 MB, macOS/Linux/Windows)
- ✅ Public landing page with 9 GIF demos
- ✅ Multilingual landing in 9 languages (EN + 8 EU)
- ✅ Architecture documentation with diagrams
- ⏳ iOS Xcode wrapper needs regeneration (M1)
- ⏳ Country picker, Overview screen, Comparison mode are placeholders (M1–M2)

## 8. Use of provided funds

100% covers project lead time (independent contractor, EU resident, invoiced against milestone acceptance):

- 350 hours @ €60/hour blended = €21,000
- 30 hours external accessibility audit (M6, third-party EU a11y consultant) = €2,500
- Signing certs + store submission fees (M5) = €1,000
- Buffer (~2%) = €500
- **Total: €25,000**

No agency overhead. No fixed costs beyond certs. All deliverables ship under AGPL-3.0-or-later on a public GitHub repo. CI pipeline is GitHub Actions (free for OSS).

## 9. Other support

None currently. This project is bootstrapped on the author's evenings + weekends since 2026. NLnet would be the first external support and the trigger to move it from "ships when it ships" to "ships on a milestone schedule with a public roadmap."

The author would happily co-list NLnet as the primary funder on the landing page and in release notes, and write a project diary post for the NGI Zero blog on milestone completion.

## 10. Comparison to other tools (more depth, optional)

The Eurostat dissemination API publishes JSON-stat 2.0 responses with up to 6+ dimensions (geo × time × age × sex × unit × indicator). Existing public consumers:

- **eurostat.cz**, **destatis.de**, **insee.fr** — each statistical office runs its own browser dashboards. None are mobile-native, none compare across countries.
- **PyJStat**, **rjstat** — language-specific parsers. Powerful for analysts, useless for non-coders.
- **Tableau / Power BI** — proprietary, $$$, requires a published cube.

EU Stats Multiplatform aims at the empty quadrant: **mobile-first + free + open-source + EU-data-first + multi-form-factor**.

## 11. Long-term vision

If M1–M6 ship cleanly, the natural next phases (potentially future NLnet milestones or community-driven):

- **Phase 7**: Comparison sets (saved multi-country bookmarks)
- **Phase 8**: Story mode (annotated guided tours through indicators — for journalism / education)
- **Phase 9**: Federated data sources (national statistical offices alongside Eurostat — Destatis, INSEE, GUS, etc.)
- **Phase 10**: Embedded chart export (SVG / PNG with citation watermark) — for journalists publishing in print

None of these phases are scoped for this application. They are noted to demonstrate that the architecture is built for growth, not throwaway.

## 12. Standards / open formats used

- **JSON-stat 2.0** ([specification](https://json-stat.org)) — the canonical EU statistical format. Our parser is a clean-room implementation of the public spec.
- **Eurostat REST API** — public, documented, free, no key required.
- **OpenAPI** descriptions: planned for M5 as part of CI docs.
- **WCAG 2.1 AA → AAA**: tracked across M3/M6.
- **CC-BY 4.0** for cited data; **AGPL-3.0-or-later** for code; **CC-BY-SA 4.0** considered for the design documentation in `design/`.

## 13. Communication channels

- Source: https://github.com/romantsisyk/MyEuroStatApp
- Issues: https://github.com/romantsisyk/MyEuroStatApp/issues
- Email: roman.tsisyk1@gmail.com
- Licensed under AGPL-3.0-or-later — anyone may fork and self-host

## 14. Where did you hear about NGI / NLnet?

[Fill in personally — likely via NGI Zero Commons publicity, EU FOSS news, or contributor referral.]

---

## Notes for the apply form

- The form has a **1000-character limit on most fields**. Sections 3, 5, and 7 above already fit within typical NLnet field limits. Sections 10–13 are optional / bonus and can be elided if the form is short.
- NLnet may ask for a **video** in addition. The slide-deck at `/docs/presentation/` can be screen-cast for ~5 min — see `RELEASE_HARDENING_CYCLE_REPORT.md` for the production checklist.
- Typical NGI Zero Commons amounts: €5k–€50k. Our €25k ask is at the median; we'd accept €15k–€20k with proportional milestone reduction (drop M3 or M6).
- **Submission window:** NLnet runs ~3 application rounds per year. Check https://nlnet.nl/propose/ for the next open call before submitting.
- **Response time:** NLnet typically responds within 6–8 weeks of the close of an application round.

---

## Checklist before submitting

- [ ] Repo is public on GitHub
- [ ] LICENSE file is present (AGPL-3.0-or-later) ✅ already committed
- [ ] README.md has the disclaimer (independent, not affiliated) ✅ already
- [ ] CODE_OF_CONDUCT.md is present ✅ already
- [ ] SECURITY.md is present ✅ already
- [ ] CONTRIBUTING.md is present ✅ already
- [ ] Landing page is live on GitHub Pages
- [ ] At least one GIF demo per module ✅ already (9 GIFs)
- [ ] Architecture diagrams ✅ already (`/docs/technical/`)
- [ ] Multilingual landing ✅ already (9 languages)
- [ ] All sensitive data excluded from public repo ✅ already (`.private/` gitignored)
- [ ] Recent commit history is clean ✅ already (single squashed commit)
- [ ] Public funding form filled, screenshots verified

When all checked → submit at https://nlnet.nl/propose/
