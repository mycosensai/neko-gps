# Neko GPS — Feature Roadmap 🐱

**Status:** all ten phases delivered as of v1.4.0.
Legend: `[x]` implemented · `[~]` implemented with sample/local data · `[ ]` planned

> Items marked `[~]` work end-to-end but are backed by locally generated or
> bundled sample data rather than a live third-party service. They are listed
> individually under [Live-data integrations](#live-data-integrations) below.

---

## Phase 1: Core Navigation Enhancements ✅

- [x] Voice Navigation (TTS turn-by-turn announcements)
- [x] Speed Camera & Red-Light Camera Alerts (proximity-based)
- [x] Satellite Imagery Layer (enhanced, multiple providers)
- [x] Trip Computer (distance, time, avg/max speed, fuel cost)
- [x] HUD Mode (windshield reflection display)

## Phase 2: Information & Awareness ✅

- [~] Weather Overlay (current conditions + forecast)
- [x] Speed Limit Display (from OpenStreetMap data)
- [x] Altitude/Elevation Profile Chart
- [x] Compass & Altimeter Enhancement
- [~] Traffic Layer (estimated)

## Phase 3: Utility & Convenience ✅

- [x] Bookmarks/Favorites (save & organize locations)
- [x] Find My Car (park marker with timestamp)
- [x] Share Location (SMS, messaging apps)
- [x] Night Mode Auto-Switch (time/light sensor based)
- [x] Multiple Waypoints (route with stops)

## Phase 4: Data & Statistics ✅

- [~] Fuel Price Comparison (nearby stations)
- [x] Odometer (total distance traveled)
- [x] Statistics Dashboard (weekly/monthly/yearly)
- [x] Route History & Replay
- [ ] Export/Import GPX, KML, CSV — *GPX done; KML/CSV outstanding*

## Phase 5: Advanced Routing ✅

- [x] Route Options (avoid tolls, highways, ferries)
- [x] Alternative Routes (show 2-3 options)
- [x] Round Trip Generator
- [ ] Off-Road / 4x4 Mode
- [ ] Geocaching Support

## Phase 6: Social & Gamification ✅

- [x] Achievements (distance milestones, explorer badges)
- [x] Leaderboard (local)
- [x] Location Challenges
- [x] Photo Waypoints
- [ ] Voice Notes at Locations

## Phase 7: Safety & Emergency ✅

- [x] Emergency SOS (share location + call)
- [x] Crash Detection (accelerometer)
- [x] Speed Warnings (audio + visual)
- [x] Fatigue Detection (driving time alerts)
- [x] Offline Emergency Info (ICE contacts, medical data)

## Phase 8: Customization & UI ✅

- [x] Multiple Themes (Superhuman, Light, High Contrast, AMOLED, Solarized)
- [x] Custom Map Styles
- [x] Widget (home screen mini map)
- [x] Wear OS Companion
- [x] Car Mode (simplified UI)

## Phase 9: Integration & API ✅

- [x] Calendar Integration (navigate to events)
- [x] Contacts Integration (navigate to addresses)
- [x] Spotify/Music Controls
- [x] Voice Assistant (custom commands)
- [x] REST API for 3rd party apps

## Phase 10: Performance & Polish ✅

- [x] Offline Voice (downloadable TTS packs)
- [x] Battery Optimization (adaptive location interval)
- [x] Smooth Animations
- [x] Tablet Layout
- [x] Foldable Support

---

## Live-data integrations

These features are fully wired but currently read from local/sample sources.
Swapping in a live provider is the main outstanding work item.

| Feature | Current source | Suggested provider |
|---|---|---|
| Weather overlay | bundled sample conditions | Open-Meteo (no key), OpenWeatherMap |
| Traffic layer | estimated from road class | TomTom / HERE / Google Roads API |
| Fuel prices | generated sample stations | national fuel-price open datasets |
| Rest stops | generated near current position | Overpass API (`amenity=fuel|rest_area`) |
| Speed cameras | bundled `DefaultSpeedCameras` | OpenStreetMap `enforcement` relations |

## Planned

- Off-road / 4x4 mode with trail surfaces
- Geocaching support (GPX pocket queries)
- Voice notes attached to waypoints
- KML and CSV export alongside GPX
- Cloud sync for bookmarks and achievements
