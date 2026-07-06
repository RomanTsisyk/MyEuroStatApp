# Generative AI use disclosure — EU Stats Multiplatform application

**Required by NLnet under the [generative AI policy](https://nlnet.nl/foundation/policies/generativeAI/).**

Per the policy: "applicants may use GenAI tools in preparing applications, but any such use must be disclosed [...] the model used, dates and times of prompts, the prompts themselves, the unedited output."

This disclosure covers GenAI use across the entire project — including code, documentation, web landing pages, translations, and the application materials themselves — so the NLnet panel has full transparency.

---

## Model used

**Anthropic Claude** (Sonnet and Opus model families, accessed via the Claude API and Claude Code CLI) was used throughout the project lifecycle from initial scaffolding through release hardening. Specific model versions varied across sessions and the spec refresh cycle (Sonnet 4.5 / 4.6, Opus 4 / 4.5).

No other generative AI tool (ChatGPT, Gemini, Copilot, Cursor, etc.) was used in any role of this project.

---

## What GenAI was used for

### 1. Code generation and refactoring (April – May 2026)

Claude was used as a pair-programming partner for:
- Initial multi-module Gradle scaffolding (parallel sonnet subagents per module)
- Eurostat API service implementations for each of the 8 feature modules
- JSON-stat 2.0 parser (test cases handwritten by author; parser logic AI-assisted)
- Stale-while-revalidate repository implementations (consistent pattern across 8 modules)
- Decompose component scaffolding per feature
- UI Screen composables (Compose Multiplatform)
- The 444 unit tests were generated AI-assisted, then reviewed and fixed by the author where they were wrong
- Two release-hardening QA passes that surfaced and fixed real bugs (a11y tap targets, year-picker integration regressions)

Every line of AI-generated code was reviewed, run through compiler + tests, and edited by the author before commit. The author treats AI output as a draft that always needs verification.

### 2. Documentation (May 2026)

- CLAUDE.md, README.md, NEXT_STEPS.md, RELEASE_HARDENING_CYCLE_REPORT.md — authored by the human, structured drafts generated AI-assisted
- The technical architecture reference at `/docs/technical/index.html` with inline SVG diagrams — drafted by Sonnet then edited
- Per-module screen descriptions — Sonnet-drafted from screenshot analysis, fact-checked by human against the live app

### 3. Web landing page (May 2026)

- `/docs/index.html` — initial draft built by Opus from a detailed human-written design brief; reviewed by a second Opus session; then human-edited for fact corrections (e.g. "13 modules" → "17 Gradle modules"; "444 tests" → confirmed against real test runs)
- 8 EU-language translations of the landing — 4 parallel Sonnet sessions translated DE+FR, IT+ES, PL+UK, NL+PT respectively. Each translation was reviewed for: lang attribute correctness, hreflang self-reference, canonical URL, technical-term policy (dataset codes stay English), locale-appropriate idioms (pt-PT not pt-BR; Polish without Anglicisms; Ukrainian using 2019 orthography)
- The interactive slide-deck presentation at `/docs/presentation/` was assembled by Claude from screenshot descriptions; voice-over scripts within it are AI-drafted and would be re-recorded in the author's own voice for any video production

### 4. This NLnet application package

- The form-field answers in `01-form-fields.md` — drafted by Claude (Sonnet 4.6 / 4.7 family, sessions on 26 May 2026) based on a brief from the author; structure, milestone scoping, and budget numbers reflect the author's actual plan
- This disclosure document — also Claude-drafted, then author-verified

### 5. Continued development (July 2026)

Development continued after the May application draft, using the newer Claude
model families (Sonnet 5, Opus 4.8, and the Fable writing model) via the Claude
Code CLI. AI-assisted, author-reviewed work in this window included: the
`feature-overview` dashboard module (component + screen + tests); expanding the
suite from 444 to 499 unit tests; fixing a JVM-only stdlib call that had blocked
iOS Native compilation; adding and green-ing a GitHub Actions CI workflow; and a
live-API re-verification of all 19 Eurostat dataset codes that caught and fixed
two upstream schema changes. The refinements to this application package in this
folder — including the factual updates and prose edits in July — were likewise
AI-assisted and author-reviewed. As before, every change was compiled, tested,
and reviewed by the author before commit.

---

## Dates and times (rounded; exact timestamps available on request)

| Date range | Activity | Approx. session count |
|---|---|---|
| 12–16 May 2026 | Initial Gradle scaffolding, JSON-stat parser, first feature modules | ~12 sessions |
| 17–23 May 2026 | Feature-module completion, Compose UI, design system polish | ~15 sessions |
| 24–25 May 2026 | First release-hardening pass, accessibility tap-target fixes | 3 sessions |
| 25 May 2026 | YearDropdown component + integration via 7 parallel subagents | 1 large session |
| 25 May 2026 | Landing page (Opus build + Opus review + fixes) | 1 session |
| 25 May 2026 | 8 EU-language translations + technical reference page (5 parallel subagents) | 1 session |
| 25–26 May 2026 | Slide-deck presentation from 45 screenshots (3 parallel description agents) | 1 session |
| 26 May 2026 | This NLnet application package | 1 session |
| Jul 2026 | Overview module, +55 tests, iOS-compile + CI fixes, live-API dataset re-verification, application-package refinements | ~2 sessions |

Total: estimated 32–37 sessions over roughly 14 active days (May), with a short follow-up cycle in July 2026.

---

## Sample prompts (representative, abbreviated)

**Code:**
```
"You are senior Kotlin Multiplatform release engineer performing a FINAL RELEASE HARDENING PASS before public OSS launch and NLnet submission. [...] STRICT RULES: NEVER perform large refactors. Every code change must be tied to a reproducible defect or concrete risk. [...] Phase 1 DISCOVERY: scan for Eurostat data risks (wrong dimension names, missing TOTAL filters, last-cell-wins risks), silent-failure risks (catch Throwable, runCatching, swallowed exceptions), UI state risks, accessibility gaps, dead code, release risks."
```

**Documentation:**
```
"You are designing and implementing the public landing page for a Kotlin Multiplatform open-source project [...] Quality bar: Linear · Stripe · Vercel · Apple Health · Things 3 — production-grade marketing site, not a wireframe [...] Use the project's actual design tokens [colors, typography, spacing tables provided verbatim from the source]"
```

**Translation:**
```
"Translate a single-file HTML landing page into Polish (PL) and Ukrainian (UK) [...] Voice: calm, editorial. Apple Health / Stripe / Linear style. No marketing fluff [...] Polish: use formal-but-warm tone typical of Polish tech docs. Avoid English-borrowed nouns where Polish has a clean equivalent. Ukrainian: use modern post-2019 spelling. Use 'застосунок' rather than 'додаток' for 'application'."
```

**This disclosure:**
```
"Write a complete GenAI disclosure for NLnet covering the project's AI use end-to-end — code, docs, landing page, translations, application drafting. Be honest about scope. Match NLnet's verbatim policy requirement: model, dates and times, prompts, unedited output."
```

---

## Unedited output samples

Two representative unedited Claude outputs from May 26 2026 sessions are saved at the project's private archive (`.private/genai_unedited_samples/` — not in the public repo for verbatim length). The author can supply these to the NLnet panel on request as plain-text files for evaluator scrutiny.

If the panel requires the full transcript: the author maintains complete session logs and can provide them via a private channel (PGP-encrypted email or a temporary shared folder).

---

## Author's note on transparency

The author chose to make heavy AI use rather than less. Reasoning: NLnet explicitly allows it, the productivity multiplier is real for a solo maintainer, and the quality of the result speaks for itself (499 passing tests, hardened landing page, 9 language coverage). The author equally chose to disclose comprehensively rather than minimally because NLnet's panel deserves accurate information to evaluate fairly relative to other applications.

The author treats AI output as a draft requiring verification, never as ground truth. Every claim about test counts, file counts, dataset codes, language conventions, and architectural details in the application has been independently verified by the author against the actual codebase, the actual Eurostat API documentation, and the actual rendered landing page.
