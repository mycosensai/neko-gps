# 🐱 Neko GPS

A private, feature-rich Android GPS navigation app — no accounts, no telemetry,
no third-party map API keys. Built with Kotlin and OpenStreetMap.

**Status:** v1.4.0 · min SDK 24 (Android 7.0) · target SDK 34 (Android 14)

---

## ✨ Features

### Navigation & mapping
| Feature | Description |
|---|---|
| Real-time tracking | Fused location provider backed by a foreground service |
| OpenStreetMap | osmdroid tiles — no API key required, multiple tile sources |
| Turn-by-turn | Destination search with distance, ETA and speed display |
| Route options | Avoid tolls / highways / ferries; alternative and round-trip routing |
| POI search | Restaurants, hospitals, fuel, hotels, parks via Nominatim |
| Offline maps | Tile caching for offline viewing |
| Track recording | GPX import/export with replay animation |

### Driving & safety
| Feature | Description |
|---|---|
| Speed limit & warnings | Visual, audio and spoken alerts with configurable thresholds |
| Speed cameras | Proximity alerts with a local camera database |
| Fatigue detection | Break reminders every N hours with nearby rest-stop suggestions |
| Crash detection | Automatic incident detection with configurable response |
| Emergency SOS | One-tap SMS/call to emergency contacts, location attached |
| Offline emergency data | ICE contacts, blood type, allergies, medications — available with no signal |
| HUD | Heads-up display mode for windshield projection |

### Trips & insights
| Feature | Description |
|---|---|
| Trip computer | Live distance, duration, average and max speed |
| Odometer | Lifetime distance tracking |
| Statistics dashboard | Trip analytics with charts |
| Fuel price comparison | Nearby station prices |
| Elevation profile | Route elevation graph |
| Weather overlay | Conditions along the route |

### Personalisation & platform
| Feature | Description |
|---|---|
| Themes & map styles | Night mode, custom tile sources, dark palette |
| Car mode | Distraction-free driving UI |
| Bookmarks & waypoints | Saved places, photo waypoints |
| Find my car | Automatic parking location capture |
| Share location | Send a live position link |
| Widgets & Wear OS | Home-screen widget and watch companion |
| REST API server | Optional local HTTP endpoint for automation |
| Foldable & tablet | Adaptive layouts for large and hinged screens |

---

## 🚀 Quick start

```bash
git clone https://github.com/mycosensai/neko-gps.git
cd neko-gps/native-android

# Point the build at your SDK (git-ignored)
echo 'sdk.dir=/path/to/Android/Sdk' > local.properties

./gradlew assembleDebug          # debug APK
./gradlew assembleRelease        # signed release APK
./gradlew test                   # unit tests
```

Install and launch:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.nekogps.app.debug/com.nekogps.app.MainActivity
```

### Requirements

| Tool | Version |
|---|---|
| JDK | 17 |
| Android SDK | API 34, build-tools 34.0.0 |
| Gradle | 8.4 (use the committed wrapper) |

---

## 📚 Documentation

| Document | Contents |
|---|---|
| [`docs/DEVELOPMENT.md`](docs/DEVELOPMENT.md) | Full build, test, signing and release guide |
| [`CHANGELOG.md`](CHANGELOG.md) | Release history |
| [`tools/README.md`](tools/README.md) | Emulator setup, hardware acceleration, graphics backends |
| [`FEATURE_ROADMAP.md`](FEATURE_ROADMAP.md) | Feature roadmap and delivery status |

**Signing a release?** Credentials live in `local.properties` (git-ignored) or
environment variables — see [`docs/DEVELOPMENT.md` §5](docs/DEVELOPMENT.md#5-signing-a-release).

---

## 🔧 Tech stack

| Component | Version |
|---|---|
| Kotlin | 1.9.10 (kapt for Room) |
| Android Gradle Plugin | 8.1.2 |
| Gradle | 8.4 |
| osmdroid | 6.1.17 |
| Play Services Location | 20.0.0 |
| Room | 2.5.2 |
| DataStore | 1.0.0 |
| Material Design 3 | 1.10.0 |

### Architecture

Features live in isolated packages under `app/src/main/java/com/nekogps/app/features/`,
each owning its managers, activities and layouts. Room handles structured records
(bookmarks, waypoints, parking, gamification) with schemas exported to
`app/schemas/`; DataStore holds preferences. See
[`docs/DEVELOPMENT.md` §8](docs/DEVELOPMENT.md#8-architecture).

---

## 🎨 Design system

**Superhuman-inspired** dark theme:

| Token | Hex | Role |
|---|---|---|
| Mysteria Purple | `#1b1938` | Deep background |
| Lavender Glow | `#cbb7fb` | Accent highlights |
| Charcoal Ink | `#292827` | Cards and surfaces |
| Warm Cream | `#e9e5dd` | Text and buttons |
| Amethyst Link | `#714cb6` | Links |
| Parchment Border | `#dcd7d3` | Dividers |

---

## 📦 Releases

Signed APKs are published on the
[releases page](https://github.com/mycosensai/neko-gps/releases).

| Version | APK | Highlights |
|---|---|---|
| [v1.4.0](https://github.com/mycosensai/neko-gps/releases) | `NekoGPS-v1.4.0-release.apk` | Room codegen fix, signing config, lint gate, docs |
| [v1.3.0](https://github.com/mycosensai/neko-gps/releases/tag/v1.3.0) | 3.66 MB | Safety, customisation, integrations, performance |
| [v1.2.0](https://github.com/mycosensai/neko-gps/releases/tag/v1.2.0) | 3.56 MB | Data, route intelligence, gamification |
| [v1.1.0](https://github.com/mycosensai/neko-gps/releases/tag/v1.1.0) | — | Voice, speed cameras, trip computer, HUD |

---

## ⚠️ Known constraints

- **Emulator use requires hardware virtualisation.** x86/x86_64 images will not
  boot without Intel VT-x / AMD-V enabled in firmware plus the AEHD driver.
  See [`tools/README.md`](tools/README.md) for diagnosis steps.
- Fuel prices, speed cameras and rest stops currently use locally generated
  sample data; wiring them to live providers is on the roadmap.

---

## 📝 License

MIT — your private GPS app, nya~ 🐱

---

Made with ❤️ by Neko-chan using [Hermes Agent](https://hermes-agent.nousresearch.com/docs)
