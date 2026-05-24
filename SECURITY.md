# Security Policy

## Supported versions

This project is in active development. Only the latest commit on `main` is supported. Tagged releases will be supported once we ship 1.0.

## Reporting a vulnerability

If you discover a security issue, please email **roman.tsisyk1@gmail.com** directly rather than opening a public issue.

We aim to acknowledge reports within 7 days. If the issue is confirmed, we'll coordinate disclosure with you before publishing a fix.

## Scope

This is a read-only client app for the public Eurostat API. Specifically:

- **No user authentication** — no accounts, no tokens, no passwords
- **No PII collection** — no analytics, no tracking, no user data sent anywhere
- **No payment data** — the app is free and has no commerce
- **HTTPS only** — all API calls go to `ec.europa.eu` over TLS

Likely security surfaces worth looking at:

- **Cache integrity** — the SQLDelight DAOs in `core-database` and per-module cache implementations
- **Network handling** — Ktor client configuration in `core-network/EurostatApiClient.kt`
- **JSON parsing** — `core-jsonstat/JsonStatParser.kt` (handles untrusted input from Eurostat)
- **Dependency vulnerabilities** — we use pinned versions in `gradle/libs.versions.toml`; please open issues for CVE alerts on packages we depend on

## Out of scope

- The Eurostat API itself (report to the EU directly)
- The Android / iOS runtime
- The Kotlin standard library or Compose Multiplatform framework

## Acknowledgements

Reporters who follow this policy and act in good faith will be credited (with their permission) in the release notes for the fix.
