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
- Release signing pointed at a non-existent keystore path (`../release.keystore`
  resolved outside the project), so release artefacts were not signed with the
  intended key.
- Removed two orphaned helper classes (`SafetyCrashHelper`,
  `SafetyEmergencyContactsHelper`) that duplicated logic already present in
  `SafetyActivity` and broke compilation by reaching into private members.
- Restored `private` visibility on 21 `SafetyActivity` methods that had been
  inadvertently widened.

### Security
- Removed hard-coded keystore passwords from `app/build.gradle`. Signing
  credentials are now read from `local.properties` (git-ignored) or environment
  variables, and the signing config is only registered when the keystore
  actually exists.

### Fixed (release-only)
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
- Lint ran with `abortOnError false`, so nothing caught the above. Lint is now
  a hard gate (see below) and the project is lint-clean with no baseline.

### Added
- 31 JVM unit tests. `DatabaseCodegenTest` is a regression guard for the Room
  bug above: it opens both databases in memory, asserts every DAO is present,
  loads the generated `*_Impl` classes by name, and round-trips a bookmark
  through the generated DAO.
- `LICENSE` (MIT), matching what the README already claimed.
- `.github/workflows/android.yml` — CI running tests, lint and both APK builds
  on every push and pull request.
- `docs/DEVELOPMENT.md` — build, test, sign and release guide.
- `tools/emulator.sh` + `tools/README.md` — emulator setup, hardware
  acceleration diagnosis and graphics-backend guidance.
- `CHANGELOG.md`.

### Changed
- `versionName` / `versionCode` now match the shipped release train (1.4.0 / 4);
  previously the manifest still advertised 1.0.0 while 1.3.0 was released.
- Debug builds use the application id suffix `.debug` and a `-debug` version
  suffix so debug and release installs can coexist on one device.
- Release builds enable `shrinkResources` in addition to R8 minification.
- Lint is now a build gate (`abortOnError true`, baseline-backed) instead of
  `abortOnError false`, which silently allowed broken code to ship.
- Added `BuildConfig.GIT_SHA` and `BuildConfig.BUILD_TIME` for traceable builds.
- Room now exports schemas to `app/schemas` so migrations are reviewable in git.
- `gradle.properties` sets `android.nonTransitiveRClass` and parallel/daemon
  defaults for faster local iteration.

### Added
- Test dependencies for Robolectric-backed unit tests, AndroidX Test, Espresso
  and coroutine test support.
- `docs/DEVELOPMENT.md` — how to set up, build, test, sign and release.
- `tools/` directory holding the emulator helper scripts that previously
  littered the repository root.

### Removed
- Committed scratch artefacts: per-run detekt report dumps, emulator logs,
  a 67 MB detekt CLI jar and 11 MB `gh.zip`, all of which bloated the repo.

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
