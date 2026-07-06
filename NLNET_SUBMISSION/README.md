# NLnet submission package — EU Stats Multiplatform

**Target fund:** NGI Zero Commons Fund
**Deadline:** 1 June 2026 12:00 CEST
**Apply at:** https://nlnet.nl/propose/

## Files in this package

| File | Purpose |
|---|---|
| `01-form-fields.md` | **Main submission text.** Copy-paste answers field by field into the NLnet propose form. |
| `02-genai-disclosure.md` | **Required by NLnet.** Full GenAI use disclosure to paste into field #18 + upload as attachment. |
| `03-milestones.md` | Detailed milestone descriptions (referenced from main form, can also be uploaded as attachment). |
| `attachments/` | Optional file uploads — HTML/PDF only, 50 MB total limit. |

## Pre-flight checklist (must be done before submitting)

- [ ] **Push repo to public GitHub** — currently local-only. Run:
      `git remote add origin https://github.com/RomanTsisyk/<REPO>.git && git push -u origin master`
- [ ] **Activate GitHub Pages** — Settings → Pages → Branch `master` → Folder `/docs`
- [ ] **Verify landing loads** at `https://RomanTsisyk.github.io/<REPO>/`
- [ ] **Update placeholder URLs** in `01-form-fields.md` (replace `RomanTsisyk` and `<REPO>` with actual GitHub handles)
- [ ] **Get a PGP key (optional)** — NLnet lets you submit one for encrypted comms with reviewers. Not required.
- [ ] **Confirm the requested amount** — see the private budget note (`.private/nlnet-budget-figures.md`, gitignored); figures are entered directly in the NLnet form, not committed here. Sized for a solo contributor over 4 months; high approval probability.

## Submission steps

1. Open https://nlnet.nl/propose/ in browser
2. Section by section, paste from `01-form-fields.md`
3. In the "Generative AI" section: paste content from `02-genai-disclosure.md` AND attach it as a file (NLnet wants both)
4. (Optional) Attach `02-genai-disclosure.md`, `03-milestones.md`, and/or a snapshot of `docs/technical/index.html` in the project attachments slot
5. Check both privacy + email-copy checkboxes
6. Submit

## What NLnet will do next

- Acknowledgement email within ~24 h
- Review by the NGI Zero Commons fund panel
- Decision typically within 6–8 weeks of the deadline (so early August 2026)
- If approved, you'll be invited to sign a Statement of Work with concrete milestones

## Key reference numbers

| Metric | Value |
|---|---|
| Codebase | 18 Gradle modules, ~17 000 lines Kotlin (rough estimate) |
| Tests | 499 unit tests, 0 failures |
| Modules | 8 thematic feature modules shipping real Eurostat data + an Overview dashboard |
| Datasets | 19 official Eurostat datasets, re-verified against the live Eurostat API |
| Platforms | Android, iOS (KMP common code compiles — verified), macOS, Linux, Windows |
| Languages (web) | 9 (English + 8 EU translations of the landing) |
| Build | Android APK ships at ~20 MB; desktop UberJar at ~99 MB |
| License | AGPL-3.0-or-later |
| Disclaimer | Independent third-party. Not affiliated with Eurostat or the European Commission. |
