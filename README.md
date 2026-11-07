# Sairen

An offline, local-file music player for Android. Native Kotlin + Jetpack Compose, AndroidX Media3 for
playback, no network permission at all.

Sairen reads the audio files that Android's MediaStore already indexed on your device, plays them
through a `MediaSessionService` so playback survives leaving the app, and keeps the whole experience
on-device: no accounts, no streaming, no ads, no analytics, no telemetry, no Internet permission.

> **Verification status (please read):** the whole app **builds and tests green in CI**. GitHub Actions
> (`.github/workflows/android.yml`, run `42239888376` on 2026-11-06) ran the real Gradle build:
> `:app:compileDebugKotlin` and `:app:compileDebugUnitTestKotlin` succeed, **140 unit tests execute in 22
> classes with 0 failures and 0 skipped**, `lintDebug` passes, and **both** `assembleDebug` and
> `assembleRelease` produce APKs — `app-debug.apk` (15,774 KiB) and `app-release.apk` (2,773 KiB, R8 +
> resource shrinking, signed with the debug key) — whose `lib/` contents were inspected: only
> `arm64-v8a` and `armeabi-v7a`. Five real defects that compiling exposed were fixed rather than papered
> over (two in phase 2, three in phase 3 — see
> [CI build and the bugs it caught](#4-ci-build-and-the-bugs-it-caught)). What is still *not* verified:
> installing the APK, actual playback, notifications/lock-screen controls, any on-device rendering, and
> whether the release build behaves like the debug build at runtime. There is no device or emulator here,
> so **no claim is made about real-world smoothness**; the jank work is described by what it changes in
> the code, not by a measured frame time. Earlier in authoring only the framework-free half could be
> exercised (a real Kotlin compiler, 118 pure-logic tests); that sandbox evidence is kept in
> [Verification](#verification-what-was-actually-run).

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
- [CI build and the bugs it caught](#4-ci-build-and-the-bugs-it-caught)
- [Known limitations](#known-limitations)
- [Attribution](#attribution)

---

## Features

| Area | What Sairen does |
| --- | --- |
| Discovery | MediaStore query with `IS_MUSIC != 0`, `DURATION >= 30s`, `SIZE >= 100 KiB`; embedded album art via `content://media/external/audio/albumart/<albumId>`, with a placeholder when the device has no art |
| Library | Songs / Albums / Artists / Playlists tabs, album and artist detail screens, derived groupings (no extra cursors). The first scan is **silent**: no spinner, banner or "scanning" text anywhere in the startup path |
| Collections | Chips over the song list: All, Liked, Recently added (newest first, capped at 100), Most played (zero-play tracks hidden), Recently played, Not played. Play and Shuffle act on the filtered list only |
| Likes | Heart toggle on every song row, in the track actions sheet, on Now Playing and in the mini player; liked ids persist in DataStore |
| Playlists | Create, rename, delete, add/remove songs, reorder with up/down; playlist detail screen with Play/Shuffle and a "N missing" note when files disappeared |
| Play counts | One count per listen, awarded only after 30 s or half the track, whichever comes first — never on start, seek or resume |
| Search | Debounced (300 ms), case-insensitive, token-based match over title + artist + album, ranked (title prefix → title → artist → album) |
| Transport | Play, pause, next, previous (restarts the track when >3 s in), seek bar with elapsed/total |
| Modes | Shuffle and repeat off/all/one, driven by the Media3 player so the notification stays in sync |
| Queue | Reorder (up/down), remove, play next, add to queue; play any queue item directly |
| History | Recently played (last 50) persisted in DataStore, shown as a shelf and a full list |
| Visuals | Near-black surfaces with a `#1DB954` accent, rounded cards and chips, a 2 dp progress hairline in the mini player, and a Now Playing backdrop tinted from the cover art (computed on a 24×24 decode, no new dependency) |
| Background | `MediaSessionService` + `MediaSession`: notification, lock-screen and Bluetooth/headset controls, Android Auto/assistant compatible |
| Audio behaviour | Audio focus handled by ExoPlayer (`handleAudioFocus = true`), pause on headphone unplug (`setHandleAudioBecomingNoisy(true)`), last queue/position restored paused on next launch |
| Theming | Dark, Light and System default, persisted in DataStore |
| Responsive | Compact / Medium / Expanded window classes: bottom navigation vs. navigation rail, 2-column vs. adaptive album grid, single-pane vs. two-pane Now Playing with a dockable queue |

## Screens

1. **Songs** — recently played shelf + collection chips + the filtered song list.
2. **Albums** — responsive grid; **Albums detail** — header with play/shuffle + track list in disc order.
3. **Artists** — list; **Artist detail** — album shelf + all songs.
4. **Playlists** — every playlist with a per-row menu (rename, delete); **Playlist detail** — Play/Shuffle,
   total duration, reorderable rows, like and remove actions.
5. **Search** — full-screen field with grouped results (albums → artists → songs).
6. **Now Playing** — expandable (`slideInVertically` from the mini player), large artwork over a gradient
   tinted from that artwork, a scale animation tied to play state, heart toggle, seek bar, transport row,
   queue sheet (phones) or third pane (tablets).
7. **Recently played** — the persisted history.
8. **Settings** — theme, notification status, rescan (the only place a scan shows any feedback), about.
9. **Permission gate** — rationale before the system dialog; "Open settings" when permanently denied.

## Tech stack

- **Kotlin** 2.2.10, **Compose** (BOM 2025.08.01, Material 3), **Navigation Compose** 2.9.0
- **AndroidX Media3** 1.8.0 (`media3-exoplayer`, `media3-session`)
- **DataStore Preferences** 1.1.7 (settings + history + last session; no Room, no KSP)
- **Coil** 2.7.0 for artwork (`coil-compose`; pure Kotlin/Java — the built APK's JNI libraries come
  from `androidx.graphics.path` and DataStore, not Coil; see [ABI notes](#abi-notes))
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
│   ├── LikedTracks.kt           # pure: liked-id set (de)serialisation
│   ├── LikedStore.kt            # DataStore likes facade
│   ├── PlayCountRules.kt        # pure: when a listen counts (30 s or half the track)
│   ├── PlayCounts.kt            # pure: play-count map (de)serialisation
│   ├── PlayCountStore.kt        # DataStore play-count facade
│   ├── Playlist.kt              # pure: Playlist + PlaylistCodec + PlaylistEditor (add/remove/move)
│   ├── PlaylistStore.kt         # DataStore playlist facade
│   ├── SongCollections.kt       # pure: the six song-list projections
│   ├── MusicLibrary.kt          # repository interface used by ViewModels
│   ├── MusicRepository.kt       # in-memory index + recent history
│   ├── RecentPlayedStore.kt     # DataStore history facade
│   ├── RecentPlayedList.kt      # pure history maths + (de)serialisation
│   ├── PlaybackSessionStore.kt  # DataStore last-session facade
│   ├── SavedSession.kt          # pure SavedSession + SavedSessionCodec
│   ├── SettingsStore.kt         # DataStore theme facade
│   ├── ThemeMode.kt             # pure theme enum + storage mapping
│   ├── ThemePreferences.kt      # abstraction used by SettingsViewModel
│   └── DataStoreProvider.kt     # single process-wide Preferences DataStore
├── playback/
│   ├── PlaybackService.kt       # MediaSessionService: ExoPlayer + MediaSession + notification
│   ├── PlayerConnection.kt      # MediaController wrapper → StateFlow<PlayerUiState>
│   ├── PlayerUiState.kt         # UI model + RepeatMode
│   ├── PlayerProgress.kt        # position/duration only, published at 500 ms
│   ├── MediaItems.kt            # Track → MediaItem mapping (album id in extras)
│   └── QueueEditor.kt           # pure queue index maths (move/remove/play-next)
└── ui/
    ├── SairenApp.kt             # scaffold, top bar, mini player, nav bar/rail, nav graph
    ├── navigation/              # string routes
    ├── theme/                   # palette, typography, pure ThemeResolution
    ├── components/              # artwork, track row (+like), actions sheet, playlist dialogs,
    │                            # mini player, artwork palette/gradient, common bits
    ├── library/                 # library ViewModel + songs/albums/artists/playlists/detail/recent screens
    ├── player/                  # Now Playing + queue + PlayerActions
    ├── search/                  # SearchViewModel + SearchIndex + screen
    ├── settings/                # SettingsViewModel + screen
    └── permissions/             # Permissions helpers, pure PermissionDecision, rationale gate
```

Pure, framework-free logic (`RecentPlayedList`, `SavedSessionCodec`, `QueueEditor`, `SearchIndex`,
`ThemeMode`, `PermissionDecision`, formatters, window size classes) is deliberately kept out of the
Android/Compose files so it can be compiled and unit tested on a plain JVM — that is the part covered by
the executed tests below.

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

# Release build (R8 minify + resource shrinking are on, debug-key signed so it installs)
./gradlew assembleRelease
# → app/build/outputs/apk/release/app-release.apk
```

Install:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The Gradle wrapper is the official one from the `gradle/gradle` repository at tag `v8.14.3`; the
wrapper downloads `gradle-8.14.3-bin.zip` on first run (needs network for that one step and for Maven
artifacts — the *app* needs no network at runtime and has no `INTERNET` permission).

## Testing

`./gradlew test` runs the JVM unit tests (JUnit 4 + `kotlinx-coroutines-test`), no device required:

| Test | Covers |
| --- | --- |
| `QueueEditorTest` | move up/down, play-next insertion maths, refusal to remove the playing item, current-index bookkeeping |
| `SearchIndexTest` | case-insensitivity, multi-token AND matching, ranking, album/artist grouping, limits |
| `SearchViewModelTest` | 300 ms debounce with virtual time, rapid typing, immediate clear, library updates |
| `LibraryViewModelTest` | refresh on init, album ordering by track number, artist grouping, recent-id → track resolution, missing ids |
| `SettingsViewModelTest` | theme read/write, no-op write when unchanged |
| `PermissionDecisionTest` | rationale → first denial → permanent denial → settings round trip, one-shot notification prompt |
| `RecentPlayedListTest` / `SavedSessionCodecTest` / `ThemeModeTest` | history cap/dedupe/tolerant decode, session round trip and malformed payloads, enum storage mapping |
| `LibraryGroupingTest` | counts, durations, sorting, artwork inheritance |
| `FormattersTest` | `m:ss` / `h:mm:ss`, negative/unknown durations, totals |
| `WindowSizeTest` / `ThemeResolutionTest` | 600/840 dp breakpoints, system-vs-explicit theme resolution |
| `PlayCountRulesTest` | 30 s vs. half-duration, whichever is smaller; unknown/zero durations; nothing counted on start, seek or resume |
| `PlayCountsTest` | count payload round trip, corrupt and out-of-range input, monotonic increment |
| `LikedTracksTest` | liked-id set round trip, ordering, malformed payloads |
| `PlaylistCodecTest` / `PlaylistEditorTest` | playlist payload round trip, name escaping, add/remove/move maths, duplicate ids, missing tracks |
| `SongCollectionsTest` | all six collections: ordering, the 100-track cap, zero-play filtering, dedupe, unresolved ids |
| `ArtworkPaletteTest` | accent extraction: alpha and luminance filters, saturation weighting, channel floor, fallback colour |
| `LibraryActionsViewModelTest` | like/playlist/play-count changes through the ViewModel seam, including a missing track |

All 22 test classes (**140 tests**) run in CI on every push, and the workflow publishes the numbers
(class by class) as a check-run annotation. `LibraryViewModelTest` (6), `SearchViewModelTest` (4) and
`SettingsViewModelTest` (3) are the coroutine-ViewModel tests: they use `kotlinx-coroutines-test` with a
`MainDispatcherRule` and virtual time, and they compile and pass in CI (`0 failed, 0 skipped`).
`testing/` also holds three helper files that are not test classes (`MainDispatcherRule`,
`FakeMusicLibrary`, `TestTracks`).

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

Phase 3 added **no permission**: likes, play counts and playlists live in the app's own DataStore file,
which needs none.

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
  `assembleDebug` produces **one universal APK** that carries both 32-bit and 64-bit ARM code.
- **Measured in CI**, not assumed: `app-debug.apk` is 15,774 KiB, `app-release.apk` (R8) is 2,773 KiB, and
  both APKs' `lib/` contains exactly four native libraries, all of them ARM:

  | Library | ABI | Size |
  | --- | --- | --- |
  | `libandroidx.graphics.path.so` | `arm64-v8a` | 10,096 B |
  | `libdatastore_shared_counter.so` | `arm64-v8a` | 7,112 B |
  | `libandroidx.graphics.path.so` | `armeabi-v7a` | 7,252 B |
  | `libdatastore_shared_counter.so` | `armeabi-v7a` | 4,416 B |

  They come from `androidx.graphics:graphics-path` (a transitive Compose dependency) and from DataStore's
  shared counter. Sairen itself contains no C/C++ and calls no JNI directly. There are **no x86/x86_64**
  libraries, because the ABI filters drop every other ABI. An earlier revision of this file claimed the
  APK would have an empty `lib/` folder — that was a reading of the dependency list, and the first real
  build falsified it; the numbers above are what the build actually produced.
- Total native payload is ~28 KiB, so the "one universal APK" trade-off costs nothing measurable.
- Verify in your build with:

  ```bash
  unzip -l app/build/outputs/apk/debug/app-debug.apk | grep -E 'lib/|\.so' || echo "no native libs"
  unzip -l app/build/outputs/apk/release/app-release.apk | grep -E 'lib/|\.so' || echo "no native libs"
  ```

  The CI workflow prints the same listing and turns it into a check-run annotation, so the contents are
  auditable from the run page.
- 32-bit ARM devices run exactly the same Kotlin code; the two JNI libraries above ship both
  `armeabi-v7a` and `arm64-v8a` builds, so neither ABI is short-changed.

## Performance notes

Phase 3 went looking for the causes of the reported jank instead of guessing, and changed only what was
demonstrably wasteful:

- **Playback position no longer rebuilds the UI.** `PlayerConnection` used to emit a whole
  `PlayerUiState` copy five times a second, which recomposed the mini player, the queue and Now Playing
  on every tick. Position now travels on its own `PlayerProgress` flow, consumed *only* by the seek bar
  (`SeekSection`) and by `MiniPlayerProgressLine` — in the common case a tick redraws one 2 dp line.
  `PlayerUiState` is rebuilt on four Media3 events only: timeline, media-item transition, shuffle and
  metadata.
- **No work per recomposition.** `Artwork` remembers its Coil `ImageRequest` per `(uri, size)` instead of
  building a new request (and cache key) on every frame, and asks Coil for the size it will draw at
  rather than decoding full-resolution covers into list rows.
- **The startup scan is invisible, so it also stops churning the tree.** No loader swaps in and out while
  MediaStore is queried; the list appears once, with stable `LazyColumn` keys (`track.id`,
  `entry.track.id`, `playlist.id`), so scroll position and item state survive collection switches.
- **Colour extraction stays off the main thread.** The Now Playing accent decodes a 24×24 bitmap via Coil
  in a `produceState` coroutine, averages it with a pure-Kotlin function, and applies one
  `Brush.linearGradient`; no Palette dependency, no `MediaMetadataRetriever`, no bitmap kept alive.
- **Tap handlers do no allocations**: the collection projection is memoised with `remember` on its inputs,
  so tapping a row plays the already-computed list rather than re-sorting the library.
- Cold start does no I/O on the main thread: `AppContainer` only constructs stores; the MediaStore scan
  runs on `Dispatchers.IO` after the first frame, and grouping runs on `Dispatchers.Default`.
- One cursor query per scan; no per-track queries, no polling. Search precomputes lowercase haystacks once
  per library load.
- Release builds enable R8 (`isMinifyEnabled`) and resource shrinking; the dependency list is still seven
  runtime artifacts plus the Compose BOM — the palette was written by hand rather than adding one.

**Honest gap:** none of the above was measured on a device. The debug build carries `isDebuggable`, so
Compose skips its own optimisations there; `app-release.apk` exists precisely so the same code can be
judged with R8 on, but its runtime behaviour is **untested** in this environment (see below).

## Verification: what was actually run

**Environment:** Debian-based sandbox. No JDK, no Android SDK, no Gradle distribution preinstalled.
Outbound network is allow-listed to `github.com`, `codeload.github.com`, `api.github.com`,
`registry.npmjs.org`, `pypi.org`, `files.pythonhosted.org` only.

### 1. Toolchain probing

| Command | Outcome |
| --- | --- |
| `curl -s -o /dev/null -w "%{http_code}" https://repo1.maven.org/` | `000` (blocked) — also `000` for `repo.maven.apache.org`, `maven.google.com`, `dl.google.com`, `services.gradle.org`, `downloads.gradle.org`, `plugins.gradle.org`, `jitpack.io`, `cdn.jsdelivr.net` |
| `pip download jdk4py==17.0.9.2` | **succeeded** → Temurin JDK 17.0.9 (`java -version` → `openjdk 17.0.9`) |
| `curl https://api.github.com/…/gradle/wrapper/gradle-wrapper.jar?ref=v8.14.3` | **succeeded** — official 43,764-byte wrapper jar, valid JAR (`unzip -l`), `sha256 7d3a4ac4…df296172`; `gradlew` + `gradlew.bat` fetched the same way |
| Gradle distribution / Android SDK / build-tools | **not obtainable** — hosting hosts blocked, no `sdkmanager` |
| AGP, AndroidX, Media3, Compose, Coil artifacts | **not obtainable** — Maven Central and Google Maven blocked |
| `kotlin-compiler` from npm (`registry.npmjs.org`) | **succeeded** → `kotlinc-jvm 2.4.21 (JRE 17.0.9)` |
| `junit-4.13.2.jar` / `hamcrest-core-1.3.jar` | **not obtainable** — Maven Central blocked; the GitHub-hosted copies are Git LFS pointers, so a minimal local stand-in for `@Test`/`Assert` was used (assertion semantics identical for the assertions used) |

### 2. Static checks (all passed)

| Check | Result |
| --- | --- |
| XML well-formedness across all 38 XML files (manifest, values, values-night, values-v29, drawables, mipmaps, `xml/`) | **0 errors** |
| Resource-reference resolution (`R.string/drawable/color/mipmap/xml/style` used in Kotlin, manifest and XML) | **0 missing** (117 strings, 28 drawables, 2 adaptive icons, 2 colors, 1 style) |
| Brace/paren/bracket balance per Kotlin file (94 files, strings + comments stripped) | **0 imbalances** |
| Project-internal `import com.sairen.player.*` resolution | **0 unresolved** (the crude scan flags only the generated `R` class and extension-property imports such as `sairenDataStore` / `findActivity`, which have no top-level declaration to match) |
| Package declaration vs. directory | **0 mismatches** |

### 3. Real compiler + test execution (framework-free half)

```bash
java -jar /tmp/kc/package/lib/kotlin-compiler.jar -nowarn -no-stdlib \
     -classpath /tmp/kc/package/lib/kotlin-stdlib.jar -d /tmp/verify/out <sources> <tests>
# → exit 0, no errors (19 production files + 18 test-source files, phase 3)
java -cp /tmp/verify/out:<stdlib> verify.RunnerKt <18 test classes>
```

Result (verbatim, phase 3):

```
TESTS PASSED: 118, FAILED: 0
```

Across 18 classes: `LibraryGroupingTest` (5), `LikedTracksTest` (5), `PlayCountRulesTest` (6),
`PlayCountsTest` (8), `PlaylistCodecTest` (7), `PlaylistEditorTest` (15), `RecentPlayedListTest` (5),
`SavedSessionCodecTest` (4), `SongCollectionsTest` (12), `ThemeModeTest` (2), `QueueEditorTest` (12),
`RepeatModeTest` (5), `ArtworkPaletteTest` (7), `PermissionDecisionTest` (8), `SearchIndexTest` (9),
`ThemeResolutionTest` (2), `FormattersTest` (4), `WindowSizeTest` (2).

Compiled production files: `data/model/Models.kt`, `data/{PlayCountRules,PlayCounts,LikedTracks,Playlist,
SongCollections,LibraryGrouping,RecentPlayedList,ThemeMode,SavedSession}.kt`,
`playback/{QueueEditor,PlayerUiState,PlayerProgress}.kt`, `ui/search/SearchIndex.kt`,
`ui/components/ArtworkPalette.kt`, `ui/permissions/AudioAccessState.kt`,
`ui/util/{SairenWindowSize,Formatters}.kt`, `ui/theme/ThemeResolution.kt` — plus their test classes.
Because the compiler is a *real* Kotlin compiler, this also type-checks every declaration those files use.
The other 4 classes in the CI total (22 − 18 = `LibraryActionsViewModelTest`, `LibraryViewModelTest`,
`SearchViewModelTest`, `SettingsViewModelTest`) need DataStore/Android stubs and are CI-only.

### 4. CI build and the bugs it caught

The sandbox above could not run Gradle, so the whole Android layer was compiled for the first time on
GitHub's runners. The first CI runs (`42210231759`, `42210361824`, `42210656856`) all failed in the
"Unit tests" step. Because the Actions job log is served from a signed object-store URL that is not
reachable from here, the workflow was extended with `.github/scripts/ci_diagnostics.py`, which converts
Gradle/Kotlin/aapt2/lint output and the JUnit XML into **check-run annotations**. That is how the real
errors were read, verbatim:

1. **Broken platform theme reference (resource linking).**
   `values-v29/themes.xml` used `parent="android:Theme.DeviceDefault.DayNight.NoActionBar"`. The platform
   has no such style (AOSP defines `Theme.DeviceDefault.DayNight`, and `NoActionBar` variants exist only
   for other `DeviceDefault` themes), so aapt2 failed with

   ```
   ERROR: AAPT: error: resource android:style/Theme.DeviceDefault.DayNight.NoActionBar not found.
   error: failed linking references.
   ```

   `:app:processDebugResources` failed — and because `testDebugUnitTest`, `lintDebug` and `assembleDebug`
   all depend on it, that single error is what made the *unit test* step fail before a single test ran.
   Fixed by parenting on `android:Theme.DeviceDefault.DayNight` and suppressing the action bar with
   `windowActionBar=false` / `windowNoTitle=true` (the day/night launch window is preserved).

2. **Material 3 API used outside its scope (Kotlin).** With resources linking, compilation proceeded and
   `:app:compileDebugKotlin` failed with

   ```
   e: SairenApp.kt:375:5 Unresolved reference 'NavigationBarItem'.
   e: SairenApp.kt:379:13 @Composable invocations can only happen from the context of a @Composable function
   ```

   Material 3 declares `NavigationBarItem` as `RowScope.NavigationBarItem`, so it cannot be called from a
   standalone composable. The local wrapper is now `private fun RowScope.TopLevelNavItem(...)`, which is
   what its three call sites (all inside `NavigationBar { }`) already were.

After both fixes, run **`42212827921`** was green end to end for the phase-2 feature set.

Phase 3 added a lot of new code, and three more real defects surfaced the same way — again found by the
compiler, not by reasoning:

3. **AppCompat-only attribute in the new vector drawables (resource linking).** `ic_heart`,
   `ic_heart_filled`, `ic_playlist` and `ic_add` were written with `android:tint="?attr/colorControlNormal"`.
   This is a Compose-only app with no AppCompat dependency, so the attribute does not exist and aapt2
   refused to link:

   ```
   /drawable/ic_heart.xml:6: error: resource attr/colorControlNormal (aka com.sairen.player:attr/colorControlNormal) not found.
   error: failed linking file resources.
   ```

   Fixed by deleting the attribute; Compose tints every icon at runtime (`Icon(tint = …)`), which is what
   the pre-existing drawables already relied on.

4. **A missing repository override.** `MusicRepository` implemented `recordCompletedPlay(...)` but not the
   `PlayCountsStore.incrementPlayCount(trackId)` member it had inherited, so it was not abstract and did
   not implement the interface. Fixed by collapsing the two names into the contract name; the service now
   calls `incrementPlayCount`.

5. **`Flow` vs. `StateFlow` in the library interface.** `MusicLibrary` inherited
   `likedIds: Flow<Set<Long>>` from its stores while `LibraryViewModel` exposes
   `StateFlow<Set<Long>>`, which does not type-check (`Initializer type mismatch: expected
   'StateFlow<Set<Long>>', actual 'Flow<Set<Long>>'`). Every implementation caches a `StateFlow` anyway,
   so the interface now narrows the three collections to `StateFlow`, and the UI can collect them without
   inventing an initial value.

After those fixes, run **`42239888376`** is green end to end — unit tests, lint, **both** APK variants, the
APK contents report and the artifact upload. Annotations from that run, verbatim:

```
Unit test results : Executed 140 tests in 22 classes, 0 failed, 0 skipped.
Debug APK report  : APKs: app-debug.apk (15774 KiB), app-release.apk (2773 KiB)
                    lib/arm64-v8a/libandroidx.graphics.path.so
                    lib/arm64-v8a/libdatastore_shared_counter.so
                    lib/armeabi-v7a/libandroidx.graphics.path.so
                    lib/armeabi-v7a/libdatastore_shared_counter.so
```

No test was deleted, skipped, weakened or disabled in any phase. Every fix above is a source change that
addresses the actual error.

### 5. What is explicitly **not** verified

| Item | Status |
| --- | --- |
| `./gradlew testDebugUnitTest`, `lintDebug`, `assembleDebug` | **RUN — green in GitHub Actions** (run `42239888376`); still **not run locally**, because the sandbox has no Gradle distribution, Android SDK or Maven access |
| `assembleRelease` (R8 minify + resource shrinking, debug-key signed) | **RUN — green in CI**, `app-release.apk` 2,773 KiB. Whether the minified build behaves the same at runtime is **NOT TESTED** |
| Both APKs' `lib/` inspection | **RUN** — `arm64-v8a` + `armeabi-v7a` only, 4 native libraries, artifact uploaded |
| Real-world jank / frame timings, debug vs. release | **NOT MEASURED** — no device. The perf changes are verified as code (single-consumer position flow, remembered requests, stable keys), not as numbers |
| Release-build runtime (R8-renamed classes, reflection, Media3 service lookup) | **NOT TESTED** — untested territory; the minified APK has never been launched |
| Likes / play counts / playlists persistence round trip on a real device | **NOT TESTED** — the codecs and DataStore mapping are unit tested, the on-device store is not |
| Kotlin compilation of the Android/Compose/Media3 layer | **RUN** — `compileDebugKotlin` and `compileDebugUnitTestKotlin` succeed in CI |
| Manifest merger output, `foregroundServiceType` in the merged manifest | **NOT VERIFIED** — needs a merged-manifest artifact |
| Install, playback, notification/lock-screen controls, permission dialogs, headphone-unplug pause, process-death resume | **NOT TESTED** — no emulator, no device, no `adb` |
| Compose UI rendering / responsive layouts on phones, foldables and tablets | **NOT TESTED** — the APK has never been rendered |

## Known limitations

- **Built, but never run.** Unit tests, lint and the debug APK build are green in CI; the APK has
  never been installed, so runtime behaviour (playback, service lifecycle, notifications, UI rendering)
  is still unproven until someone runs it on a device.
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
- A play is counted at 30 seconds **or** half the track, whichever is shorter, so a 20-second track counts
  after 10 seconds and a 10-minute track after 30. Tracks that never reach that point are never counted —
  that is the requested rule, not an accident.
- Playlist reordering uses up/down buttons (same reason as the queue): no drag-and-drop, so a long
  playlist takes several taps to reorder. There is also no drag-to-reorder for the queue.
- Likes, play counts and playlists are keyed by MediaStore `_ID`. If Android re-indexes a file (new id),
  that track loses its heart and its count. Ids that no longer resolve are skipped silently in playlists
  and counted as "N missing" in the header.
- `app-release.apk` is signed with the **debug keystore** so it can be installed for performance testing.
  It is not a distributable, upgradeable release build.
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
