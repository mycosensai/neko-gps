# Changelog

All notable changes to Neko GPS are documented here.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Fixed
- **Room database code generation was never running — every database access
  crashed at runtime.** Two compounding defects: the build used
  `annotationProcessor` instead of `kapt` (so no `*_Impl` classes were ever
  generated for the Kotlin `@Database`/`@Dao` types), and Room 2.5.2 cannot
  parse Kotlin 1.9 metadata, so `suspend` DAO functions were mis-read as
  returning `java.lang.Object` and `val` constructor properties as needing
  setters. Any code path touching bookmarks, saved parking, waypoints,
  achievements or the leaderboard would have thrown
  `IllegalStateException: Cannot find implementation for ... AppDatabase_Impl`.
  Fixed by applying `kotlin-kapt`, routing Room's compiler through `kapt`, and
  upgrading Room to 2.6.1. The APK now contains 14 generated DAO/database
  implementations (previously zero).
- **Background tracking silently stopped whenever the map screen closed.**
  `LocationTrackingService` was only ever *bound* (`BIND_AUTO_CREATE`), never
  foreground-*started*, so the system destroyed it when the UI unbounded. It
  now exposes `onStartCommand` (`START_STICKY`) and is started with
  `ContextCompat.startForegroundService`, keeping the tracker and its
  notification alive in the background.
- **The app crashed at startup on devices without Google Play Services.**
  `NekoGpsApp`, the tracking service, navigation, HUD and trip computer all
  called `LocationServices.getFusedLocationProviderClient` unconditionally,
  which throws on GMS-less devices. New `LocationClients` helper returns null
  when Play Services is absent; `LocationTrackingService` now falls back to the
  platform `LocationManager`, and `NekoGpsApp` reads the platform's last known
  fix instead of crashing.
- **The tracking notification was invisible on Android 13+.** `POST_NOTIFICATIONS`
  was declared but never requested; it is now part of the first-run permission
  request.
- **Package visibility broke runtime intent resolution on Android 11+.**
  `resolveActivity` for the camera and share intents returned null without a
  `<queries>` declaration, silently disabling photo waypoints and some share
  options. Manifest now declares the intents.
- osmdroid never called `Configuration.load(...)`, so tile caches targeted the
  external-storage path that is not writable on Android 10+.
- **Gson generic deserialisation would have broken only in release builds.**
  The ProGuard rules did not preserve the `Signature` attribute, so
  `TypeToken<List<…>>` lost its type argument and JSON would silently
  deserialise into `LinkedTreeMap` instead of the expected model. The rules
  also kept a package that does not exist (`com.nekogps.app.data.**`) while
  leaving the real Gson-bound models unguarded, so R8 was free to rename their
  fields. Rules now keep `Signature` plus the speed-camera, fuel, route-point
  and emergency-data models explicitly.
- Unresolvable layout constraint in `activity_alternative_routes.xml`: the
  routes `RecyclerView` constrained its bottom edge to `@id/btnSelectRoute`,
  a child of a sibling `CardView` rather than a sibling of the RecyclerView.
  The constraint could never resolve, so the list mis-sized at runtime. Now
  points at the sibling `@id/cvSelectRoute`.
- Release signing pointed at a non-existent keystore path (`../release.keystore`
  resolved outside the project), so release artefacts were not signed with the
  intended key.

### Security & privacy
- **Launcher icon was a system resource** (`@android:drawable/ic_menu_mylocation`)
  and the mipmap folders were empty — the app had no real icon. A branded
  adaptive icon set (purple paw on Mysteria Purple) with full PNG fallbacks for
  API 24–25 is now shipped; generation script kept in `tools/generate_icons.py`.
- **Backup was enabled and included sensitive data** — medical info, emergency
  contacts, location history and waypoints were eligible for Google cloud
  backup. `allowBackup` is now false: for a "mine only" GPS app the data stays
  on-device. (A user-controlled export/import feature is the intended
  replacement; see roadmap.)
- **Global cleartext traffic was allowed** while every network call in the app
  is HTTPS. Replaced with a `networkSecurityConfig` that refuses cleartext
  except on localhost (the optional REST API server).
- Removed hard-coded keystore passwords from `app/build.gradle`. Signing
  credentials are now read from `local.properties` (git-ignored) or environment
  variables, and the signing config is only registered when the keystore
  exists.
- Removed three unused dangerous permissions (`BODY_SENSORS`,
  `HIGH_SAMPLING_RATE_SENSORS`, `FOREGROUND_SERVICE_HEALTH`).

### Changed
- `versionName` / `versionCode` now match the shipped release train (1.4.0 / 4);
  previously the manifest still advertised 1.0.0 while 1.3.0 was released.
- Debug builds use the application id suffix `.debug` and a `-debug` version
  suffix so debug and release installs can coexist on one device.
- Release builds enable `shrinkResources` in addition to R8 minification.
- Lint is now a hard build gate (`abortOnError true`, no baseline) instead of
  `abortOnError false`, which silently allowed broken code to ship. The project
  is currently lint-clean.
- Added `BuildConfig.GIT_SHA` and `BuildConfig.BUILD_TIME` for traceable builds.
- Room now exports schemas to `app/schemas` so migrations are reviewable in git.
- `gradle.properties` sets `android.nonTransitiveRClass` and parallel/daemon
  defaults for faster local iteration.
- Restored `private` visibility on 21 `SafetyActivity` methods and removed two
  orphaned helper classes that duplicated existing logic.

### Added
- 31 JVM unit tests. `DatabaseCodegenTest` is a regression guard for the Room
  bug above: it opens both databases in memory, asserts every DAO is present,
  loads the generated `*_Impl` classes by name, and round-trips a bookmark
  through the generated DAO. `DistanceCalculatorTest` covers distance, bearing,
  ETA and formatting maths.
- `LICENSE` (MIT), matching what the README already claimed.
- `.github/workflows/android.yml` — CI running tests, lint and both APK builds
  on every push and pull request (pending `workflow` token scope).
- `docs/DEVELOPMENT.md` — build, test, sign and release guide.
- `tools/emulator.sh`, `tools/README.md` and `tools/generate_icons.py`.
- `CHANGELOG.md`.

### Removed
- Committed scratch artefacts: per-run detekt report dumps, a 67 MB detekt CLI
  jar, emulator logs, `gh.zip` and ad-hoc emulator batch scripts.

## [1.3.0] — 2026-09-10

### Added
- Phase 7–10 feature set: safety suite (SOS, crash detection, fatigue
  detection, speed warnings, offline emergency/ICE data), customisation
  (themes, map styles, car mode), integrations (calendar, contacts, music,
  REST API server, voice assistant, Wear OS companion) and performance work
  (adaptive location intervals, battery optimisation, foldable/tablet layouts,
  offline TTS, animations).
- 69 Kotlin source files and 39 layouts across the `features/` tree.

### Changed
- Static analysis pass: `SwallowedException`, `UnusedPrivateProperty`,
  `UnusedImports` and `TooGenericExceptionCaught` reduced to zero.
- All `MagicNumber`, `LongMethod` and `NestedBlockDepth` violations resolved.

## [1.2.0] — 2026-09-08

### Added
- Phase 4–6 features: fuel & odometer logging, trip statistics, route replay,
  route options and alternatives, round-trip routing, achievements,
  leaderboard, challenges and photo waypoints.

### Fixed
- Route replay point loading, `Bitmap`/`Drawable` conversion, `GeoPoint`
  conversion and a `val` reassignment in the trip computer.

## [1.1.0] — 2026-09-08

### Added
- Phase 1–3 features: voice guidance, speed camera alerts, trip computer, HUD,
  weather overlay, speed limit display, elevation profile, traffic layer,
  bookmarks, find-my-car, share location, night mode and waypoints.

## [1.0.0] — 2026-09-07

### Added
- Initial release: OpenStreetMap-backed navigation, location tracking service,
  Superhuman-inspired dark theme (Mysteria Purple `#1b1938` / Lavender Glow
  `#cbb7fb`), search and POI lookup, track recording, settings via DataStore.

[Unreleased]: https://github.com/mycosensai/neko-gps/compare/v1.3.0...HEAD
[1.3.0]: https://github.com/mycosensai/neko-gps/compare/v1.2.0...v1.3.0
[1.2.0]: https://github.com/mycosensai/neko-gps/compare/v1.1.0...v1.2.0
[1.1.0]: https://github.com/mycosensai/neko-gps/compare/v1.0.0...v1.1.0
[1.0.0]: https://github.com/mycosensai/neko-gps/releases/tag/v1.0.0
