# Contributing to EU Stats Multiplatform

Thanks for considering a contribution. This is a small, focused civic-tech project — clear PRs with a single purpose land fastest.

## Development setup

You need:

- **JDK 17 or 21** (Temurin recommended)
- **Android Studio Iguana (2023.2.1) or newer** — for the Android side
- **Xcode 15 or newer** — for iOS (optional; commonMain compiles without it)
- **macOS, Linux, or Windows** — all three should work for Android development

Clone and build:

```bash
git clone https://github.com/RomanTsisyk/eu-stats-multiplatform.git
cd eu-stats-multiplatform
./gradlew :composeApp:assembleDebug
```

The first build takes 5–10 minutes (Compose Multiplatform downloads its compiler classpath). Subsequent builds are fast.

## Tests

```bash
./gradlew allTests                              # everything
./gradlew :feature-population:allTests          # one module
./gradlew :core-jsonstat:allTests               # the JSON-stat parser
```

Tests live in `commonTest` source sets. Use `kotlin.test` + Turbine — Mockk doesn't work on iOS targets so we use hand-written fakes.

## Code style

We follow Kotlin official style conventions plus a few project-specific rules documented in [CLAUDE.md](CLAUDE.md):

- `Result<T>` with `Loading / Success / Error` for all data-layer return types
- `AppError` (sealed taxonomy) for every cross-boundary failure — never throw raw exceptions across the data/domain boundary
- `DispatcherProvider` injected — never `Dispatchers.IO` directly
- No `!!` anywhere; use `requireNotNull`, `checkNotNull`, or sealed-class exhaustive matching
- Single class per file, file name matches the public class
- KDoc on every public type, public method, and non-obvious algorithm

Before submitting a PR, please run a clean build to confirm nothing's broken:

```bash
./gradlew :composeApp:assembleDebug allTests
```

## Pull request process

1. **Open an issue first** for non-trivial changes (new features, behavior changes, refactors). One-line fixes can skip this.
2. Branch from `main`. Use descriptive branch names: `fix/cohort-overflow`, `feat/comparison-screen`.
3. Keep PRs focused — one concern per PR. If you find yourself touching 20 files for unrelated reasons, split.
4. Reference the issue number in the PR description.
5. Include a "How to verify" section: what command did you run, what output did you see, what manual check confirmed the change works.

## Reporting bugs

Use GitHub Issues. Please include:

- What you did (commands, taps, screen)
- What you expected
- What actually happened
- Environment (OS, JDK, Android SDK, device/emulator)
- Logs or screenshots if relevant

## Asking questions

Use GitHub Discussions for design questions, ideas, or "how do I…" type queries. Use Issues for confirmed bugs or concrete feature requests.

## License

By contributing, you agree that your contributions will be licensed under the AGPL-3.0 (same as the project). This includes patches, documentation, and any other content you submit.

See [LICENSE](LICENSE) for the full text.
