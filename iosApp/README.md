# iosApp

A real, working `iosApp.xcodeproj` is checked in — generated from
[`project.yml`](project.yml) with [XcodeGen](https://github.com/yonaskolb/XcodeGen).
The app builds and launches in the iOS simulator with live Eurostat data.

## Build & run

```sh
# If `xcode-select -p` points at CommandLineTools, either fix it once:
#   sudo xcode-select -s /Applications/Xcode.app/Contents/Developer
# or prefix every xcodebuild/xcrun call with:
#   DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer

cd iosApp
xcodebuild -project iosApp.xcodeproj -scheme iosApp \
  -destination 'platform=iOS Simulator,name=iPhone 17' \
  CODE_SIGNING_ALLOWED=NO ONLY_ACTIVE_ARCH=YES build
```

Or open `iosApp.xcodeproj` in Xcode, pick a simulator, ⌘R.

The "Compile Kotlin Framework" pre-build phase runs
`./gradlew :composeApp:embedAndSignAppleFrameworkForXcode`, so Gradle builds
the KMP framework first, then Xcode compiles the Swift layer.

## Regenerating the project

Edit `project.yml`, then:

```sh
brew install xcodegen   # once
cd iosApp && xcodegen generate
```

## Gotchas already handled in project.yml / sources

- **JAVA_HOME**: Xcode script phases scrub `PATH`, so the pre-build script
  pins `JAVA_HOME` to the Android Studio JBR when unset. A mismatched JDK
  corrupts Kotlin incremental caches ("Don't know how to deserialize a
  snapshot with type tag ..."); if you ever hit that, delete
  `.gradle/8.11.1/executionHistory` and rebuild.
- **`-lsqlite3`** in `OTHER_LDFLAGS`: the Kotlin framework is static and
  SQLDelight's native driver binds the system sqlite3 — the app must link it.
- **`CADisableMinimumFrameDurationOnPhone`** in `Info.plist`: required by
  Compose Multiplatform's `PlistSanityCheck`; without it the app aborts at
  startup.
- **`KoinIOSKt.doInitKoinIos()`** (not `initKoinIos`): Kotlin/Native prefixes
  `init*` functions with `do` in the ObjC/Swift export.

## File layout

```
iosApp/
├── project.yml                # XcodeGen manifest (source of truth)
├── Configuration/
│   └── Config.xcconfig        # BUNDLE_ID, APP_NAME, TEAM_ID
├── iosApp/
│   ├── iOSApp.swift           # @main entry — calls doInitKoinIos()
│   ├── ContentView.swift      # SwiftUI wrapper over MainViewController()
│   └── Info.plist
└── iosApp.xcodeproj/          # Generated — regenerate via xcodegen
```
