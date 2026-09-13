# Neko GPS — Feature Proposals

Prioritised feature ideas for the next releases, mapped to what already exists
in the codebase. Grouped by theme; each entry notes the effort and the
existing code it plugs into.

Legend: 🟢 quick win (≤1 day) · 🟡 medium (2–5 days) · 🔴 large (1–3 weeks)

---

## 1. Turn sample data into live data  — the single highest-value theme

The roadmap's biggest honesty gap: weather, fuel prices, rest stops, traffic
and speed cameras are all **generated sample data** today. The plumbing exists
(managers + UI + persistence) — only the fetch layer is fake.

| # | Proposal | Effort | Plugs into |
|---|---|---|---|
| 1.1 | **Real weather** via Open-Meteo (free, keyless, HTTPS) | 🟢 | `WeatherOverlay.kt`, `WeatherApiClient.kt` |
| 1.2 | **Real rest stops & fuel stations** via Overpass API (`amenity=fuel|rest_area|charging_station`) — the URL is already in the codebase | 🟢 | `FatigueDetectionManager.generateMockRestStops`, `FuelPriceManager` |
| 1.3 | **Community speed-camera reports** — report a camera from the map, store locally, export/import | 🟡 | `SpeedCameraManager`, `DefaultSpeedCameras` |
| 1.4 | **Real traffic** (estimated → provider) | 🟡 | `TrafficLayer.kt` |

## 2. Privacy-first features (on-brand: "mine only")

| # | Proposal | Effort | Notes |
|---|---|---|---|
| 2.1 | **Data export/import to file** — bookmarks, waypoints, tracks, settings as one encrypted JSON file | 🟡 | Replaces the cloud backup we disabled in the audit; user-controlled |
| 2.2 | **Privacy dashboard** — one screen showing exactly what is stored on-device, with per-category delete and total export | 🟡 | New screen; strong Play-Store story |
| 2.3 | **Incognito mode** — no history, no track recording, no gamification updates | 🟢 | Toggle consumed by `RecordedTrackStore`, `OdometerEntity`, gamification |
| 2.4 | **Location history with map replay** (heat map of where you've been, stored locally only) | 🟡 | Reuse `OdometerDao`/tracks |

## 3. Navigation & routing upgrades

| # | Proposal | Effort | Notes |
|---|---|---|---|
| 3.1 | **Offline turn-by-turn navigation** — cache a region's OSM data (osmdroid tiles already offline-capable), route with an on-device engine (brouter/MOTIS), voice guidance offline | 🔴 | The single biggest missing capability for a GPS app |
| 3.2 | **Android Auto** — maps + navigation surface | 🔴 | Huge for a driving app; androidx.car.app |
| 3.3 | **EV mode** — charging stations layer, battery-aware range ring, chargers as route waypoints | 🟡 | Overpass `amenity=charging_station` + existing routing |
| 3.4 | **Altitude-aware routing** — prefer flatter routes using existing elevation data | 🟡 | `ElevationProfileRenderer` data pipeline |
| 3.5 | **Hazard/incident reporting layer** (potholes, roadworks, police) — local-first, optional import | 🟡 | Reuse map overlay + persistence patterns |
| 3.6 | **Multi-day road-trip planner** — daily distance budget, overnight stops, hotels along the route | 🔴 | Extends `RoundTripActivity` |

## 4. Driving experience

| # | Proposal | Effort | Notes |
|---|---|---|---|
| 4.1 | **Stop-tracking control** — notification action + main-screen toggle to end the foreground service (today the tracker has no stop path once started) | 🟢 | `LocationTrackingService` |
| 4.2 | **Steep-grade alerts** — warn before 6%+ descents/ascents from elevation data | 🟢 | Elevation pipeline + TTS |
| 4.3 | **Overspeed history & trip score** — per-trip speeding summary, gamification hook | 🟡 | `TripComputerViewModel`, `SpeedWarningManager` |
| 4.4 | **Motorcycle mode** — HUD variant with lean-friendly layout, group-ride position share | 🟡 | `HUDActivity` |
| 4.5 | **Dashcam / trip recording with video** — camera + location burn-in | 🔴 | New service; permission-sensitive |
| 4.6 | **Night projection mode** — auto brightness + image inversion hint for windshield HUD | 🟢 | `NightModeManager`, `HUDActivity` |

## 5. Intelligence & convenience

| # | Proposal | Effort | Notes |
|---|---|---|---|
| 5.1 | **Geofenced bookmark alerts** — notify when within N meters of a saved place | 🟡 | `BookmarkManager` + FGS location stream |
| 5.2 | **Live trip share** — opt-in, expiring link others can follow (privacy-first: off by default, no account) | 🔴 | `ShareLocation` + REST API server |
| 5.3 | **"OK Neko" voice commands** — hands-free nav, cancel, report camera | 🟡 | `VoiceAssistantManager` |
| 5.4 | **Refuelling log + economy analytics** — link fuel stops to odometer, cost/km history | 🟡 | `OdometerEntity`, `FuelPriceManager` |
| 5.5 | **Material You dynamic colour** | 🟢 | `ThemeManager` |
| 5.6 | **KML/CSV export** alongside GPX | 🟢 | `GpxParser` area |

## 6. Platform & quality

| # | Proposal | Effort | Notes |
|---|---|---|---|
| 6.1 | **Instrumentation smoke test** — boot MainActivity, assert no crash, on CI emulator | 🟡 | Extends `DatabaseCodegenTest` |
| 6.2 | **Crash reporting that respects privacy** — optional, on-device-only log export instead of a third-party SDK | 🟡 | fits the "no telemetry" brand |
| 6.3 | **App bundles + Play signing** when distributing beyond sideload | 🟢 | build config only |
| 6.4 | **Widget: live ETA & distance to destination** | 🟡 | `WidgetProvider` |

---

## Recommended next release (v1.5.0) — quick wins

1. 🟢 1.1 Real weather (Open-Meteo)
2. 🟢 1.2 Real rest stops / fuel (Overpass)
3. 🟢 4.1 Stop-tracking control
4. 🟢 5.5 Material You dynamic colour
5. 🟢 5.6 KML/CSV export
6. 🟡 2.1 Data export/import (privacy headline feature)

…then a v1.6.0 focused on the two 🔴 items that define the product:
**3.1 offline navigation** and **3.2 Android Auto**.
