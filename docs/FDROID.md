# Installing EU Stats from F-Droid

[F-Droid](https://f-droid.org) is the trusted catalog of free and open source
Android apps. EU Stats Multiplatform is being submitted for inclusion; this
document tracks status and tells users how to install once the listing is
live.

## Status

EU Stats is **not yet listed in the official F-Droid catalog**. A merge
request against [`fdroiddata`](https://gitlab.com/fdroid/fdroiddata) is
prepared and will be filed alongside the first signed upstream tag
(`v0.4.0`). The build recipe lives at [`metadata/eu.eurostat.app.yml`](../metadata/eu.eurostat.app.yml)
in this repository so reviewers can inspect it ahead of submission.

The F-Droid inclusion review typically takes 2–6 weeks. Until then, the
APK is available as a [GitHub release](https://github.com/RomanTsisyk/MyEuroStatApp/releases)
attachment.

## Why F-Droid

- Reproducible builds from source — F-Droid's build server compiles the
  APK on its own infrastructure from the tag declared in
  [`metadata/eu.eurostat.app.yml`](../metadata/eu.eurostat.app.yml), so
  what you install matches exactly what the public Git history shows
- Independent verification that the app contains no proprietary
  dependencies, trackers or analytics
- Automatic updates without a Google account
- Works on devices that ship without Google Play Services
  (de-Googled Android, GrapheneOS, /e/, LineageOS)

## After listing — how to install

1. Install the F-Droid client from <https://f-droid.org/>.
2. Search for **EU Stats** (or open the app page at
   `https://f-droid.org/packages/eu.eurostat.app/` once published).
3. Tap **Install**.

## Verifying the build (advanced)

F-Droid signs the APK it ships with its own signing identity, separate
from the upstream signing key. For users who want to verify the upstream
APK against the F-Droid build:

```bash
# Download both APKs (upstream signed + F-Droid signed)
# Then diff their unsigned contents with apkdiff or apksigner verify
apksigner verify --print-certs eu.eurostat.app_40.apk
```

The expected SHA-256 of each release APK is published as part of the
GitHub release notes from `v0.4.0` onward.

## Why two signing identities are OK for this project

F-Droid policy explicitly allows the catalog to ship its own signed
build of an app that is also distributed via other channels. Because
the build server rebuilds from source, the on-device APK is binary
identical (modulo the signing block) to what an auditor would produce
locally. The upstream signing key is held by the lead maintainer and
used only for the GitHub-release attachment.

## What if the F-Droid review asks for changes?

Open issues against this repository tagged `f-droid`. Common reviewer
asks for first-time submissions:

- Pin the Gradle wrapper checksum
- Pin a specific JDK distribution in the build server profile
- Remove any blob assets from the repo (none currently present)

The maintainer commits to addressing reviewer feedback within one week
of receipt.
