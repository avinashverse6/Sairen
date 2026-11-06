# Sairen

An offline, local-file music player for Android. Native Kotlin + Jetpack Compose, AndroidX Media3 for
playback, no network permission at all.

Sairen reads the audio files that Android's MediaStore already indexed on your device, plays them
through a `MediaSessionService` so playback survives leaving the app, and keeps the whole experience
on-device: no accounts, no streaming, no ads, no analytics, no telemetry, no Internet permission.

> **Verification status (please read):** the full source tree and Gradle wrapper are committed, but
> this project was authored in a sandbox where the Android SDK, the Gradle distribution and all Maven
> repositories are unreachable, so `./gradlew assembleDebug`, `./gradlew lint` and `./gradlew test`
> could **not** be executed here. Everything that *was* verified is listed in
> [Verification](#verification-what-was-actually-run). Treat the compiler-level result as unverified
> until you run the build locally or in CI (a workflow is included).

---

## Contents

- [Features](#features)
- [Screens](#screens)
- [Tech stack](#tech-stack)
- [Architecture](#architecture)
- [Build and run](#build-and-run)
- [Testing](#testing)
- [Permissions](#permissions)
- [ABI notes](#abi-notes)
- [Performance notes](#performance-notes)
- [Verification: what was actually run](#verification-what-was-actually-run)
- [Known limitations](#known-limitations)
- [Attribution](#attribution)

---

## Features

| Area | What Sairen does |
| --- | --- |
| Discovery | MediaStore query with `IS_MUSIC != 0`, `DURATION >= 30s`, `SIZE >= 100 KiB`; embedded album art via `content://media/external/audio/albumart/<albumId>`, with a placeholder when the device has no art |
| Library | Songs / Albums / Artists tabs, album and artist detail screens, derived groupings (no extra cursors) |
| Search | Debounced (300 ms), case-insensitive, token-based match over title + artist + album, ranked (title prefix → title → artist → album) |
| Transport | Play, pause, next, previous (restarts the track when >3 s in), seek bar with elapsed/total |
| Modes | Shuffle and repeat off/all/one, driven by the Media3 player so the notification stays in sync |
| Queue | Reorder (up/down), remove, play next, add to queue; play any queue item directly |
| History | Recently played (last 50) persisted in DataStore, shown as a shelf and a full list |
| Background | `MediaSessionService` + `MediaSession`: notification, lock-screen and Bluetooth/headset controls, Android Auto/assistant compatible |
| Audio behaviour | Audio focus handled by ExoPlayer (`handleAudioFocus = true`), pause on headphone unplug (`setHandleAudioBecomingNoisy(true)`), last queue/position restored paused on next launch |
| Theming | Dark, Light and System default, persisted in DataStore |
| Responsive | Compact / Medium / Expanded window classes: bottom navigation vs. navigation rail, 2-column vs. adaptive album grid, single-pane vs. two-pane Now Playing with a dockable queue |

## Screens

1. **Songs** — recently played shelf + full song list.
2. **Albums** — responsive grid; **Albums detail** — header with play/shuffle + track list in disc order.
3. **Artists** — list; **Artist detail** — album shelf + all songs.
4. **Search** — full-screen field with grouped results (albums → artists → songs).
5. **Now Playing** — expandable (`slideInVertically` from the mini player), large artwork with a subtle
   scale animation tied to play state, seek bar, transport row, queue sheet (phones) or third pane (tablets).
6. **Recently played** — the persisted history.
7. **Settings** — theme, notification status, rescan, about.
8. **Permission gate** — rationale before the system dialog; "Open settings" when permanently denied.

## Tech stack

- **Kotlin** 2.2.10, **Compose** (BOM 2025.08.01, Material 3), **Navigation Compose** 2.9.0
- **AndroidX Media3** 1.8.0 (`media3-exoplayer`, `media3-session`)
- **DataStore Preferences** 1.1.7 (settings + history + last session; no Room, no KSP)
- **Coil** 2.7.0 for artwork (`coil-compose`; pure Kotlin/Java, ships **no** native `.so` files)
- **MVVM**: `ViewModel` + `StateFlow`, a `MusicLibrary` repository seam, manual DI via `AppContainer`
  (no Hilt: one activity, one service, a handful of ViewModels — Hilt would add a plugin, a compiler and
  startup cost for no gain)
- **minSdk 26**, **targetSdk/compileSdk 36**, JDK 17, AGP 8.13.2, Gradle 8.14.3, version catalog + Kotlin DSL

### Why not AGP 9 / compileSdk 37?

AGP 9.x (current stable at the time of writing) enables *built-in Kotlin* and removes the classic
`kotlin-android` plugin plus large parts of the old DSL, and `compileSdk 37` requires it. Because this
project could not be compiled in the authoring environment (see [Verification](#verification-what-was-actually-run)),
it deliberately pins the last well-established 8.x toolchain/API-36 combination rather than shipping an
untested AGP 9 migration. Moving to `agp = 9.5.x`, `compileSdk = 37` and removing
`alias(libs.plugins.kotlin.android)` is the documented next step; nothing in the app code depends on it.

## Architecture

```
app/src/main/java/com/sairen/player/
├── MainActivity.kt              # single activity, edge-to-edge, theme → permission gate → app
├── SairenApplication.kt         # creates AppContainer (no I/O)
├── AppContainer.kt              # manual DI graph (stores, repository, lazy PlayerConnection)
├── data/
│   ├── model/Models.kt          # Track / Album / Artist / LibrarySnapshot / LibraryState (framework-free)
│   ├── MediaStoreDataSource.kt  # one cursor query on Dispatchers.IO
│   ├── LibraryGrouping.kt       # pure album/artist derivation
│   ├── MusicLibrary.kt          # repository interface used by ViewModels
│   ├── MusicRepository.kt       # in-memory index + recent history
│   ├── RecentPlayedStore.kt     # DataStore history (+ pure RecentPlayedList)
│   ├── PlaybackSessionStore.kt  # DataStore last-session snapshot (+ SavedSessionCodec)
│   ├── SettingsStore.kt         # DataStore theme (+ ThemePreferences abstraction)
│   └── DataStoreProvider.kt     # single process-wide Preferences DataStore
├── playback/
│   ├── PlaybackService.kt       # MediaSessionService: ExoPlayer + MediaSession + notification
│   ├── PlayerConnection.kt      # MediaController wrapper → StateFlow<PlayerUiState>
│   ├── PlayerUiState.kt         # UI model + RepeatMode
│   ├── MediaItems.kt            # Track → MediaItem mapping (album id in extras)
│   └── QueueEditor.kt           # pure queue index maths (move/remove/play-next)
└── ui/
    ├── SairenApp.kt             # scaffold, top bar, mini player, nav bar/rail, nav graph
    ├── navigation/              # string routes
    ├── theme/                   # palette, typography, theme resolution
    ├── components/              # artwork, track row, actions sheet, mini player, common bits
    ├── library/                 # library ViewModel + songs/albums/artists/detail/recent screens
    ├── player/                  # Now Playing + queue + PlayerActions
    ├── search/                  # SearchViewModel + SearchIndex + screen
    ├── settings/                # SettingsViewModel + screen
    └── permissions/             # permission helpers + rationale gate
```

Data flow: `MediaStore ⇒ MediaStoreDataSource ⇒ MusicRepository (StateFlow) ⇒ LibraryViewModel ⇒ Compose`.
Playback: `PlayerConnection (MediaController) ⇄ PlaybackService (MediaSession + ExoPlayer)`.
The UI never touches Media3 types; it observes `PlayerUiState` and calls `PlayerActions`.

## Build and run

Requirements: JDK 17, Android SDK with **platform 36** and **build-tools 36.0.0**.

```bash
# Debug APK (one universal APK, no ABI splits)
./gradlew assembleDebug
# → app/build/outputs/apk/debug/app-debug.apk

# Unit tests
./gradlew test

# Android lint
./gradlew lint

# Release build (R8 minify + resource shrinking are on)
./gradlew assembleRelease
```

Install:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The Gradle wrapper is the official one from the `gradle/gradle` repository at tag `v8.14.3`; the
wrapper downloads `gradle-8.14.3-bin.zip` on first run (needs network for that one step and for Maven
artifacts — the *app* needs no network at runtime and has no `INTERNET` permission).

## Testing

`./gradlew test` runs 60+ JVM unit tests (JUnit 4 + `kotlinx-coroutines-test`), no device required:

| Test | Covers |
| --- | --- |
| `QueueEditorTest` | move up/down, play-next insertion maths, refusal to remove the playing item, current-index bookkeeping |
| `SearchIndexTest` | case-insensitivity, multi-token AND matching, ranking, album/artist grouping, limits |
| `SearchViewModelTest` | 300 ms debounce with virtual time, rapid typing, immediate clear, library updates |
| `LibraryViewModelTest` | refresh on init, album ordering by track number, artist grouping, recent-id → track resolution, missing ids |
| `SettingsViewModelTest` | theme read/write, no-op write when unchanged |
| `StoresTest` | `RecentPlayedList` cap/dedupe, `SavedSessionCodec` round-trip + malformed input, `ThemeMode` mapping |
| `LibraryGroupingTest` | counts, durations, sorting, artwork inheritance |
| `FormattersTest` | `m:ss` / `h:mm:ss`, negative/unknown durations, totals |
| `WindowSizeTest` | 600 / 840 dp breakpoints |
| `ThemeResolutionTest` | system vs. explicit light/dark resolution |

## Permissions

| Permission | Declaration | Why |
| --- | --- | --- |
| `READ_MEDIA_AUDIO` | API 33+ | Audio-only access to the MediaStore audio table |
| `READ_EXTERNAL_STORAGE` | `maxSdkVersion="32"` | The pre-33 equivalent; the cap is required so the app is not treated as a broad-storage app |
| `FOREGROUND_SERVICE` | all | Required to host playback in a foreground service |
| `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | API 34+ | Required foreground-service type for media playback |
| `POST_NOTIFICATIONS` | API 33+ | Media-style notification / lock-screen controls |

Explicitly **not** requested: `MANAGE_EXTERNAL_STORAGE`, `WRITE_EXTERNAL_STORAGE`, `INTERNET`,
`ACCESS_NETWORK_STATE`, location, `RECORD_AUDIO`. The app declares no wake lock of its own; if
`WAKE_LOCK` appears in the merged manifest it comes from Media3's playback handling, which is the
minimum needed for background audio.

Flow: an in-app rationale screen explains *why* before the system dialog appears. If the user denies
twice (system stops showing the dialog), the screen switches to a "permission is off, open settings"
state with an `ACTION_APPLICATION_DETAILS_SETTINGS` button and re-checks the grant on `ON_RESUME`.
Notifications get their own one-time rationale dialog on API 33+, and a persistent row in Settings.

### Manifest review (checked by reading the file, not by a build)

- `theme`, `icon`, `allowBackup`, `dataExtractionRules` — set; only the `datastore/` directory is backed up.
- `MainActivity` — `exported="true"` (launcher), `singleTop`, `configChanges` for rotation/size so
  playback UI does not recreate.
- `PlaybackService` — `exported="true"`, `foregroundServiceType="mediaPlayback"`, intent filters for
  `androidx.media3.session.MediaSessionService` and `android.media.browse.MediaBrowserService`.
- `androidx.media3.session.MediaButtonReceiver` — `exported="true"` with `MEDIA_BUTTON`.
- No `android:permission` on the service: Media3 restricts controller access through the session token,
  and `exported=true` is what the system media button dispatcher and Android Auto require.

## ABI notes

- `defaultConfig.ndk.abiFilters = ["armeabi-v7a", "arm64-v8a"]`, and `splits.abi` is disabled, so
  `assembleDebug` produces **one universal APK**.
- **There are no native libraries.** Sairen contains no C/C++ and none of its dependencies ship JNI
  code: Compose, Media3 (ExoPlayer's default extractors/decoders are Java/Kotlin calling platform
  `MediaCodec`), DataStore, Coil 2.7 (`coil-compose` is Kotlin/Java on top of OkHttp; the optional
  `coil-gif`/`coil-svg`/`coil-video` modules, which are also pure JVM, are not used). So the expected
  `lib/` folder in the APK is **empty/absent**; the ABI filters are a guard so a future native
  dependency cannot silently drop 32-bit ARM.
- Verify in your build with:

  ```bash
  unzip -l app/build/outputs/apk/debug/app-debug.apk | grep -E 'lib/|\.so' || echo "no native libs"
  ```

  The CI workflow prints the same line, and states so explicitly when nothing matches.
- 32-bit ARM support: every dependency above is architecture-independent, so `armeabi-v7a` devices
  (including 32-bit-only ones) run exactly the same code; no dependency needs a per-ABI variant.

## Performance notes

- Cold start does no I/O on the main thread: `AppContainer` only constructs stores; the MediaStore scan
  runs on `Dispatchers.IO` after the first frame, and the album/artist grouping runs on
  `Dispatchers.Default`.
- Lists are `LazyColumn`/`LazyVerticalGrid` with stable keys; search precomputes lowercase haystacks once
  per library load and runs on `Dispatchers.Default`.
- One cursor query per scan; no per-track queries, no polling.
- Release builds enable R8 (`isMinifyEnabled`) and resource shrinking; the dependency list is kept to
  seven runtime artifacts plus the Compose BOM.

## Verification: what was actually run

Environment: Debian-based sandbox, **no JDK, no Android SDK, no Gradle distribution**; outbound network
allowed only to `github.com`, `codeload.github.com`, `api.github.com`, `registry.npmjs.org`, `pypi.org`,
`files.pythonhosted.org`.

| Command | Result |
| --- | --- |
| `curl -s -o /dev/null -w "%{http_code}" https://repo1.maven.org/` | `000` (blocked) — same for `repo.maven.apache.org`, `maven.google.com`, `dl.google.com`, `services.gradle.org`, `downloads.gradle.org`, `plugins.gradle.org`, `jitpack.io` |
| `pip download jdk4py` | **succeeded** (a JDK *is* obtainable from PyPI) |
| Gradle distribution download | not possible (`services.gradle.org` blocked) |
| Android SDK / build-tools / platform download | not possible (`dl.google.com` blocked, no `sdkmanager`) |
| Dependency resolution (AGP, AndroidX, Media3, Compose, Coil) | not possible (Maven Central + Google Maven blocked) |
| `curl https://api.github.com/repos/gradle/gradle/contents/gradle/wrapper/gradle-wrapper.jar?ref=v8.14.3` | **succeeded** — official wrapper jar (43,764 bytes, `sha256 7d3a4ac4…df296172`), verified as a valid JAR via `unzip -l`; `gradlew`/`gradlew.bat` fetched the same way |
| `python3` XML well-formedness over all 26 XML files (manifest, resources, backup rules, themes) | **0 errors** |
| `python3` resource-reference check (`R.string/drawable/color/mipmap/xml/style` in Kotlin, manifest and XML) | **0 missing references** (82 strings, 25 drawables, 2 adaptive icons, 2 colors, 1 style) |
| Brace/paren/bracket balance per Kotlin file (59 files, strings+comments stripped) | **0 imbalances** |
| Project-internal import resolution (`import com.sairen.player.*`) | **0 unresolved** (only generated `R`, extension properties/functions flagged as false positives) |
| Package vs. directory consistency | **0 mismatches** |
| `./gradlew assembleDebug` | **NOT RUN** — impossible in this environment (no JDK installed, no Gradle distribution, no SDK, no Maven access) |
| `./gradlew lint` | **NOT RUN** — same reason |
| `./gradlew test` | **NOT RUN** — same reason; the test sources are committed and compile-checked only by inspection |
| APK `lib/` inspection with `unzip -l` | **NOT RUN** — no APK exists |
| Install/playback on emulator or device | **NOT TESTED** — no emulator, no device, no `adb` |

Because nothing was compiled, the honest expectation is: the first local `./gradlew assembleDebug` may
surface small compiler errors (a missing import, a renamed Material3 parameter, a Media3 1.8 API
difference). Please open an issue with the exact compiler output if you hit one.

## Known limitations

- **Not compiled or run in the authoring environment** (see above) — treat the build as unproven until
  you run it.
- Album art uses the legacy `content://media/external/audio/albumart/<albumId>` provider. It is what
  most devices implement, but it is not a public API: on devices that do not implement it, artwork falls
  back to the placeholder (iOS-style per-track `MediaMetadataRetriever` extraction is not implemented).
- Playback resumption restores the previous queue **paused**; it deliberately does not auto-play and does
  not implement Media3's `onPlaybackResumption` boot-resume flow.
- Queue reordering uses up/down buttons instead of drag-and-drop (accessible, no experimental APIs).
  "Play next" inserts after the current item in *logical* queue order; with shuffle enabled the visual
  order is the shuffled order.
- Removing the currently playing item from the queue is disabled to avoid an implicit track change.
- MediaStore is scanned on demand and cached in memory; there is no `ContentObserver`, so new files
  appear after a manual rescan (Settings → Rescan) or an app restart.
- No paging library: the full song list is held in memory (fine for typical libraries of a few thousand
  tracks; very large collections would benefit from Paging 3).
- No widgets, no equalizer, no sleep timer, no folder browsing, no tag editing, no playback speed —
  intentionally out of scope.

## Attribution

- Control glyphs (play, pause, skip, shuffle, repeat, queue, …) are standard Material Design icon
  path data, Apache License 2.0.
- Sairen's layout, palette application and components are original. The colour mood (near-black
  surfaces, `#1DB954` green accent) is inspired by modern music apps; no Spotify assets, icons or
  layouts are used.
