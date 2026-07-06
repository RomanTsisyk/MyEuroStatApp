# Releasing EU Stats Multiplatform

How to cut a new release. Distinct from the contributor workflow in
[`CONTRIBUTING.md`](../CONTRIBUTING.md), which covers ordinary PRs.

## Versioning

Semantic versioning: `MAJOR.MINOR.PATCH`. Pre-1.0 milestones (`0.x`) may
include breaking changes between minors; once we cut `v1.0` we promise
backwards compatibility for the public API and persisted database
schema.

`versionCode` in [`composeApp/build.gradle.kts`](../composeApp/build.gradle.kts)
is derived from the version name as `MAJOR*100 + MINOR*10 + PATCH`
(e.g. `0.4.0 → 40`, `1.0.0 → 100`).

## Signing identity

The release signing key lives outside the repo. Contributors and the
F-Droid build server fall back to the debug keystore automatically;
that is fine because F-Droid re-signs every APK with its own key
anyway, and GitHub-release attachments carry the upstream key in their
release notes for manual verification.

To produce an upstream-signed APK locally, create
`keystore.properties` in the repository root:

```properties
storeFile=path/to/release.keystore
storePassword=<store password>
keyAlias=eu-stats-release
keyPassword=<key password>
```

`keystore.properties` and `*.keystore` are already in
[`.gitignore`](../.gitignore) — never commit either.

To generate a fresh release keystore:

```bash
keytool -genkeypair \
  -v -storetype PKCS12 \
  -keystore release.keystore \
  -alias eu-stats-release \
  -keyalg RSA -keysize 4096 \
  -validity 36500 \
  -dname "CN=Roman Tsisyk, OU=EU Stats, O=EU Stats Multiplatform, L=Wroclaw, C=PL"
```

The same identity must be used for every subsequent upstream release —
Android refuses to install an upgrade signed with a different key. Back
up the keystore and the passwords somewhere durable (password manager,
hardware token, encrypted offsite copy). Losing the key means every
existing install has to be uninstalled before the next release can be
installed.

## Cutting a release

1. Land everything intended for the release on `master`.
2. Update [`CHANGELOG.md`](../CHANGELOG.md):
   move the `## [Unreleased]` block under a new
   `## [X.Y.Z] — YYYY-MM-DD` heading, and start a fresh empty
   `## [Unreleased]` above it.
3. Bump `versionName` and `versionCode` in
   [`composeApp/build.gradle.kts`](../composeApp/build.gradle.kts).
4. Add a new file `fastlane/metadata/android/{en-US,pl,uk}/changelogs/<versionCode>.txt`
   summarising the release in 1–3 sentences per locale (F-Droid reads
   these in its catalogue listing).
5. Commit and push the release-prep changes.
6. Build the upstream APK locally:
   ```bash
   ./gradlew :composeApp:assembleRelease
   sha256sum composeApp/build/outputs/apk/release/composeApp-release.apk
   ```
7. Tag the release:
   ```bash
   git tag -s vX.Y.Z -m "EU Stats Multiplatform vX.Y.Z"
   git push origin vX.Y.Z
   ```
8. Create a GitHub release for the tag and attach the APK. Paste the
   `versionCode` changelog plus the SHA-256 hash from step 6 into the
   release notes.
9. If this is a new public release: open a merge request against
   [fdroiddata](https://gitlab.com/fdroid/fdroiddata) updating
   `metadata/eu.eurostat.app.yml` to point at the new tag. See
   [`docs/FDROID.md`](FDROID.md).

## CI

GitHub Actions in [`.github/workflows/build.yml`](../.github/workflows/build.yml)
runs the test suite and assembles a debug APK on every push. The
release workflow that signs and uploads the APK to a GitHub release is
on the v1.0 roadmap (see [NEXT_STEPS.md](../NEXT_STEPS.md)).
