<div align="center">

# MyMP — Personal Music Player for Android

**A self-hosted-first music streaming client for Android, built with Kotlin & Jetpack Compose.**

[🇮🇹 Leggi in italiano](README.it.md)

</div>

---

## Screenshots

<p align="center">
  <img src="screenshots/main_screen.jpg" alt="Main screen — song list, search and mini-player" width="280"/>
  &nbsp;&nbsp;
  <img src="screenshots/settings_screen.jpg" alt="Settings screen — server configuration" width="280"/>
</p>
<p align="center">
  <em>Main screen (search, sort, playback) &nbsp;·&nbsp; Settings screen (server configuration)</em>
</p>

---

## Overview

MyMP is an Android app for streaming a personal music library from local or remote servers. It's designed to work with home servers (NAS, PC) as well as public servers distributing copyright-free or freely licensed music — content that's often unavailable on commercial streaming platforms.

Users configure up to three servers by IP address or URL, sync a song catalog, and stream tracks directly from the app — with full offline-friendly local caching via Room.

The project follows an **MVVM architecture** and was built as a hands-on exercise in production-style Android development: reactive state management, background services, local persistence, and REST networking.

---

## Table of Contents

- [Features](#features)
- [Tech Stack](#tech-stack)
- [Getting Started](#getting-started)
- [Server API](#server-api)
- [Architecture Highlights](#architecture-highlights)
- [Permissions](#permissions)
- [License](#license)

---

## Features

### Server Management
- Configure up to **3 server slots** (name + IP address/URL) from the Settings screen
- Supports both local (home network) and remote/public servers
- Active server selection via dropdown on the main screen
- Automatic URL normalization (adds `http://` if missing)

### Catalog Sync
- Fetches the song catalog via REST (`GET /manifest.json`) using Retrofit
- On-demand sync triggered from the server selector
- **Smart upsert**: existing tracks are updated in place (preserving local IDs), removed tracks are deleted — playlists are never destroyed by a re-sync
- Local Room database with per-server track isolation (`serverId`)
- Sync managed through WorkManager with automatic retry (up to 3 attempts)

### Playback
- Network audio streaming via `MediaPlayer` (local or remote sources)
- Background playback through a **Foreground Service** (`MusicService`)
- System notification with play/pause, skip, and stop controls
- Persistent **mini-player bar** at the bottom of the main screen
- Interactive **seek bar** with drag-to-scrub support
- Real-time mini-player updates on skip, track end, pause, and stop

### Playlists
- Personal playlists that are **cross-server** — tracks from different servers can coexist in the same playlist
- Add tracks via **long-press** on a song → dialog listing existing playlists + "Create new"
- Remove tracks via **long-press** → confirmation dialog
- Playlist browsing via dropdown on the main screen
- Deleting a playlist cascades and removes all its track references
- If a track is removed from the server during a re-sync, it's automatically removed from any playlists containing it (`ON DELETE CASCADE`)

### Search & Sort
- **Real-time search** by title or artist
- **4 sort modes**: Title A→Z, Title Z→A, Artist A→Z, Artist Z→A
- Visual feedback for the active sort mode via Toast
- Search and sort apply to both the server track list and the playlist view

### UI
- Forced **dark theme**, Material 3 design
- Dropdowns for server and playlist selection, with a check mark on the active item
- Currently playing track highlighted with an accent color

---

## Tech Stack

| Category | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose, Material 3 |
| Architecture | MVVM |
| Local persistence | Room |
| Networking | Retrofit |
| Background work | WorkManager, Foreground Service |
| Serialization | kotlinx.serialization |
| Navigation | Navigation Compose |

<details>
<summary>Full dependency versions</summary>

| Library | Version |
|---|---|
| Kotlin | 2.2.10 |
| AGP | 9.1.1 |
| KSP | 2.1.20-2.0.1 |
| Jetpack Compose BOM | 2025.x |
| Room | 2.7.1 |
| Retrofit | 2.x |
| WorkManager | 2.x |
| Navigation Compose | 2.8.4 |
| kotlinx.serialization | 1.x |

</details>

**Environment:** Android Studio Meerkat 2024.3.1+, JDK 17+, Gradle 9.x (managed by Android Studio)
**SDK:** minSdk 26 (Android 8.0), targetSdk 35 (Android 15), compileSdk 35

---

## Getting Started

### Build from Android Studio
1. Clone the repository
2. Open Android Studio → `File → Open` → select the project folder
3. Wait for Gradle sync to complete (first launch downloads dependencies)
4. Confirm there are no errors in the `Build` panel

### Run on an emulator
1. Open `Device Manager` in Android Studio
2. Create a Virtual Device with API 26 or higher
3. Press **Run ▶** and select the emulator
4. Note: streaming from local servers requires the emulator and server to be on the same virtual network — a physical device is recommended for full testing

### Run on a physical device
1. Enable **Developer Options** (`Settings → About phone → tap "Build number" 7 times`)
2. Enable **USB Debugging**
3. Connect via USB and authorize debugging when prompted
4. Press **Run ▶** in Android Studio and select the device

---

## Server API

MyMP expects the server to expose a `GET /manifest.json` endpoint returning a JSON array with the following structure:

```json
[
  {
    "id": 1,
    "title": "Song title",
    "artist": "Artist",
    "album": "Album",
    "filePath": "http://server-address/path/to/file.mp3"
  }
]
```

No API key is required — the app connects to HTTP servers configured manually by the user.

---

## Architecture Highlights

A few implementation details worth calling out:

- **Singleton database via `Application` class** (`MyMPApplication`): ensures a single shared Room instance across Activities and Workers, eliminating race conditions on concurrent writes
- **`MusicService` → `ViewModel` communication via shared `StateFlow`s** in `MyMPApplication`: the mini-player always reflects the actual state of the playback service, rather than an optimistic ViewModel-side state
- **Smart upsert backed by a `UNIQUE` index** on `(serverId, remoteId)` in `SongEntity`: allows track metadata to be refreshed without breaking playlist references
- **Cancellable job for the track collector** (`songsCollectionJob`): prevents multiple collectors from stacking up on rapid server switches, avoiding race conditions in the displayed list

---

## Permissions

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

`POST_NOTIFICATIONS` is requested at runtime on Android 13+.

---

## License

This project is licensed under the MIT License — see [LICENSE](LICENSE) for details.
