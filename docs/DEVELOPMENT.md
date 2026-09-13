# Neko GPS — Development & Release Guide

Everything needed to build, test, run and ship Neko GPS from a clean machine.

---

## 1. Toolchain

| Component | Version | Notes |
|---|---|---|
| JDK | 17 | Required by AGP 8.1 |
| Gradle | 8.4 | Use the committed wrapper (`./gradlew`) |
| Android SDK | API 34 (`compileSdk`/`targetSdk`) | Build tools 34.0.0 |
| Min SDK | 24 (Android 7.0) | `minSdk` |
| Kotlin | 1.9.10 | With `kapt` for Room |

### Expected locations (Windows)

```
JDK            C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot
Android SDK    %LOCALAPPDATA%\Android\Sdk
```

`local.properties` (git-ignored) must point at the SDK:

```properties
sdk.dir=C\:\\Users\\<you>\\AppData\\Local\\Android\\Sdk
```

---

## 2. Build

Always use the wrapper so everyone builds with the same Gradle.

```bash
# Debug — fast iteration, no minification
./gradlew assembleDebug

# Release — R8 + resource shrinking + signing
./gradlew assembleRelease
```

Outputs land in `app/build/outputs/apk/{debug,release}/`.

### Build types at a glance

| | Debug | Release |
|---|---|---|
| Minify (R8) | off | on |
| Resource shrinking | off | on |
| Application ID | `com.nekogps.app.debug` | `com.nekogps.app` |
| Version suffix | `-debug` | — |
| Signing | debug key | `release.keystore` |

The `.debug` suffix lets a debug and a release build live on the same device.

---

## 3. Tests

```bash
./gradlew test              # JVM unit tests (JUnit + Robolectric)
./gradlew connectedAndroidTest   # instrumentation tests (needs a device/emulator)
```

Test sources live in `app/src/test/` (JVM) and `app/src/androidTest/`
(instrumentation). `testOptions.unitTests.includeAndroidResources = true` is
enabled so Robolectric can inflate real resources.

---

## 4. Static analysis

```bash
./gradlew lintDebug                 # Android Lint (HTML + XML + text reports)
./gradlew detekt                    # Kotlin static analysis, if wired up
```

Lint is a shipping gate: `abortOnError true` backed by `app/lint-baseline.xml`.
Existing issues are captured in the baseline; **new** issues fail the build.

To intentionally accept new findings, regenerate the baseline:

```bash
./gradlew updateLintBaseline
```

### Detekt (standalone CLI)

The Gradle plugin is not wired up; detekt runs as a one-off CLI so it never
slows down normal builds. Grab the CLI jar (it is git-ignored):

```bash
curl -L -o /tmp/detekt.jar \
  https://github.com/detekt/detekt/releases/download/v1.23.8/detekt-cli-1.23.8-all.jar

java -jar /tmp/detekt.jar --input app/src/main/java --report txt:detekt/report.txt
```

Current status: `MagicNumber`, `LongMethod`, `NestedBlockDepth` and
`TooGenericExceptionCaught` are all at zero.

---

## 5. Signing a release

Signing credentials are **never** committed. Provide them in `local.properties`:

```properties
RELEASE_STORE_FILE=../release.keystore
RELEASE_STORE_PASSWORD=...
RELEASE_KEY_ALIAS=...
RELEASE_KEY_PASSWORD=...
```

…or as environment variables with the same names. The signing config is only
registered when the keystore file actually exists, so debug builds work without
any of this.

### Creating a keystore (first time only)

```bash
keytool -genkeypair -v \
  -keystore release.keystore \
  -alias nekogps \
  -keyalg RSA -keysize 2048 -validity 10000
```

> **Back this file up.** Losing it means you can never update the app on Play.

---

## 6. Running on a device

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.nekogps.app.debug/com.nekogps.app.MainActivity

adb logcat -s NekoGPS:* AndroidRuntime:E
```

### Emulator notes (important)

This project's development machine has **Intel VT-x disabled in firmware**, so
no hypervisor is available. Consequences:

- Emulator 37.x refuses to start **x86** images: *"x86 emulation currently
  requires hardware acceleration!"*
- `-accel off` (pure TCG) hangs the emulator on this machine.
- ARM images give `FATAL | QEMU2 emulator does not support arm64 CPU architecture`;
  32-bit `armeabi-v7a` is unsupported by v37 too.

**Recommended fix:** enable *Intel Virtualization Technology (VT-x)* in BIOS,
then install the AEHD driver:

```bat
:: run elevated
%LOCALAPPDATA%\Android\Sdk\extras\google\Android_Emulator_Hypervisor_Driver\silent_install.bat
```

Verify with:

```bash
emulator -accel-check
```

Helper scripts for emulator setup live in `tools/`.

**AVDs used during development**

| AVD | Image | Purpose |
|---|---|---|
| `neko` | `android-34;google_apis;x86_64` | Primary target (needs VT-x) |
| `neko24` | `android-24;default;x86` | Lightweight fallback |

---

## 7. Release checklist

1. `./gradlew clean`
2. Bump `versionCode` / `versionName` in `app/build.gradle`
3. Update `CHANGELOG.md` (move *Unreleased* entries into a versioned section)
4. `./gradlew test` — all green
5. `./gradlew lintDebug` — no new findings
6. `./gradlew assembleRelease` — verify the APK is signed:
   ```bash
   apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk
   ```
7. Smoke-test the release APK on a device (`adb install`)
8. Commit, tag, push:
   ```bash
   git commit -am "release: v1.4.0"
   git tag -a v1.4.0 -m "Neko GPS v1.4.0"
   git push origin master --tags
   ```
9. Publish the GitHub release with the APK attached.

---

## 8. Architecture

```
app/src/main/java/com/nekogps/app/
├── MainActivity.kt              # Home / dashboard
├── MapsActivity.kt              # Map surface (osmdroid)
├── NavigationActivity.kt        # Turn-by-turn navigation
├── SettingsActivity.kt          # Preference surface
├── NekoGpsApp.kt                # Application class
├── features/                    # One package per feature area
│   ├── bookmarks/               # Room: BookmarkEntity / Dao / AppDatabase
│   ├── findmycar/               # Room: ParkingLocation / Dao
│   ├── gamification/            # Room: achievements + leaderboard
│   ├── waypoints/               # Room: WaypointEntity / Dao + routing
│   ├── safety/                  # SOS, crash, fatigue, speed, offline ICE
│   ├── stats/ integration/ ui/  # fuel, calendar, themes, car mode
│   ├── performance/             # battery, TTS, foldables, animation
│   └── …
├── service/                     # LocationTrackingService (foreground)
├── offline/                     # Offline map tile cache
├── ui/                          # MapStateManager, shared UI state
└── utils/                       # Distance, GPX, coordinate conversion
```

**Data layer:** Room for structured records (bookmarks, waypoints, parking,
gamification); DataStore for simple preferences; SharedPreferences for legacy
per-feature flags. Room schemas are exported to `app/schemas/` and committed.

**Location:** `FusedLocationProviderClient` (Play Services), wrapped by
`LocationTrackingService`.

**Maps:** osmdroid with OpenStreetMap tiles; night-mode and selectable tile
sources live in `features/ui/MapStyleManager.kt`.

---

## 9. Contributing

- One logical change per commit; write imperative commit subjects.
- Update `CHANGELOG.md` under *Unreleased* for any user-visible change.
- Keep public APIs stable — many features share managers exposed as singletons.
- Prefer early returns over nested blocks; extract named constants instead of
  inline magic numbers (this is what detekt enforces).
- Never commit signing material, API keys or `local.properties`.
