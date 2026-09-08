# 🐱 Neko GPS — Private Navigation App

A fully functional, intricate Android GPS navigation app with a premium dark UI inspired by **Superhuman** (deep purple glow, minimal, confident).

## 📱 Features

| Feature | Description |
|---------|-------------|
| **Real-time GPS Tracking** | Fused location provider with foreground service for persistent tracking |
| **OpenStreetMap Maps** | osmdroid-powered, no API key needed, multiple tile layers |
| **Track Recording** | Record your path as GPX-style polylines, save & review |
| **Turn-by-Turn Navigation** | Destination search with distance, ETA, and speed display |
| **POI Search** | Find restaurants, hospitals, gas stations, hotels, parks via Nominatim |
| **Offline Support** | Map tile caching for offline viewing |
| **Settings** | DataStore-persisted preferences for map layer, GPS interval, units, theme |
| **Coordinate Conversion** | Decimal degrees ↔ DMS ↔ UTM |
| **Track Management** | List, detail view, statistics (avg/max speed, elevation) |
| **GPX Support** | Import/export tracks in GPX format |

## 🎨 Design System

**Superhuman-inspired** dark theme:

- **Mysteria Purple** `#1b1938` — deep background
- **Lavender Glow** `#cbb7fb` — accent highlights  
- **Charcoal Ink** `#292827` — cards/surfaces
- **Warm Cream** `#e9e5dd` — text & buttons
- **Amethyst Link** `#714cb6` — links
- **Parchment Border** `#dcd7d3` — dividers

## 🛠️ Build Setup

### Prerequisites
- **JDK 17** (Eclipse Temurin)
- **Android SDK** (API 34, build-tools 34.0.0)
- **Gradle 8.4**

### Build Commands
```bash
# Set environment
export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-17.0.20.101-hotspot"
export ANDROID_HOME="/c/Users/vdako/AppData/Local/Android/Sdk"

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease
```

## 📦 APK Download

`app/build/outputs/apk/release/app-release.apk` — signed release build

## 🔧 Tech Stack

| Component | Version |
|-----------|---------|
| Kotlin | 1.9.10 |
| AGP | 8.1.2 |
| Gradle | 8.4 |
| osmdroid | 6.1.17 |
| Play Services Location | 20.0.0 |
| Room | 2.5.2 |
| DataStore | 1.0.0 |
| Material Design 3 | 1.10.0 |
| Min SDK | 24 (Android 7.0) |
| Target SDK | 34 (Android 14) |

## 📝 License

MIT License — your private GPS app, nya~ 🐱

---
Made with ❤️ by Neko-chan using Hermes Agent

## Phase 4-6: Data, Route Intelligence & Gamification (v1.2.0)

### Data & Statistics
- **Fuel Price Comparison** — Compare fuel prices across nearby stations in real-time
- **Odometer** — Track total distance traveled with precision
- **Statistics Dashboard** — Comprehensive trip analytics with charts
- **Route Replay** — Animate past routes on the map with a playback slider

### Route Intelligence
- **Route Options** — Choose routes avoiding tolls, highways, or ferries
- **Alternative Routes** — Get smart route suggestions from multiple sources
- **Round Trip Generator** — Plan return trips automatically with optimized paths

### Gamification
- **Achievements** — Earn milestone-based challenges (first 1km, 100km, etc.)
- **Leaderboard** — Compete with friends on distance, accuracy, and challenges
- **Location Challenges** — Explore new areas to unlock badges
- **Photo Waypoints** — Attach geotagged photos to any location

### Build
- 69+ Kotlin files, 39+ layouts
- Build: `gradle assembleRelease` successful
- APK: `NekoGPS-v1.2.0-release.apk` (3.4MB)
