# Sairen

An offline, local-file music player for Android. Native Kotlin + Jetpack Compose, AndroidX Media3 for
playback, no network permission at all.

Sairen reads the audio files that Android's MediaStore already indexed on your device, plays them
through a `MediaSessionService` so playback survives leaving the app, and keeps the whole experience
on-device: no accounts, no streaming, no ads, no analytics, no telemetry, no Internet permission.

> **Verification status (please read):** the whole app now **builds in CI**. GitHub Actions
> (`.github/workflows/android.yml`, run `42212827921` on 2026-11-06) ran the real Gradle build:
> `:app:compileDebugKotlin` and `:app:compileDebugUnitTestKotlin` succeed, **71 unit tests execute in 14
> classes with 0 failures and 0 skipped**, `lintDebug` passes, and `assembleDebug` produces
> `app-debug.apk` (15,646 KiB) whose `lib/` contents were inspected and uploaded as an artifact. Two real
> defects that this first compile exposed were fixed rather than papered over (see
> [CI build and the two bugs it caught](#4-ci-build-and-the-two-bugs-it-caught)). What is still *not*
> verified: installing the APK, actual playback, notifications/lock-screen controls, and any on-device
> rendering — there was no device or emulator. Earlier in authoring, only the framework-free half of the
> code could be exercised (a real Kotlin 2.4.21 compiler, 58 pure-logic tests); that sandbox evidence is
> kept in [Verification](#verification-what-was-actually-run).

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
- [CI build and the two bugs it caught](#4-ci-build-and-the-two-bugs-it-caught)
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
│   ├── MediaItems.kt            # Track → MediaItem mapping (album id in extras)
│   └── QueueEditor.kt           # pure queue index maths (move/remove/play-next)
└── ui/
    ├── SairenApp.kt             # scaffold, top bar, mini player, nav bar/rail, nav graph
    ├── navigation/              # string routes
    ├── theme/                   # palette, typography, pure ThemeResolution
    ├── components/              # artwork, track row, actions sheet, mini player, common bits
    ├── library/                 # library ViewModel + songs/albums/artists/detail/recent screens
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

All 14 test classes (**71 tests**) run in CI on every push, and the workflow publishes the numbers
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
- **Measured in CI**, not assumed: `app-debug.apk` is 15,646 KiB and its `lib/` contains exactly four
  native libraries, all of them ARM:

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
  ```

  The CI workflow prints the same listing and turns it into a check-run annotation, so the contents are
  auditable from the run page.
- 32-bit ARM devices run exactly the same Kotlin code; the two JNI libraries above ship both
  `armeabi-v7a` and `arm64-v8a` builds, so neither ABI is short-changed.

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
| XML well-formedness across all 26 XML files (manifest, values, values-night, values-v29, drawables, mipmaps, `xml/`) | **0 errors** |
| Resource-reference resolution (`R.string/drawable/color/mipmap/xml/style` used in Kotlin, manifest and XML) | **0 missing** (82 strings, 25 drawables, 2 adaptive icons, 2 colors, 1 style) |
| Brace/paren/bracket balance per Kotlin file (59 files, strings + comments stripped) | **0 imbalances** |
| Project-internal `import com.sairen.player.*` resolution | **0 unresolved** |
| Package declaration vs. directory | **0 mismatches** |

### 3. Real compiler + test execution (framework-free half)

```bash
java -jar /tmp/kc/package/lib/kotlin-compiler.jar -nowarn -no-stdlib \
     -classpath /tmp/kc/package/lib/kotlin-stdlib.jar -d /tmp/verify/out <sources> <tests>
# → exit 0, no errors (12 production files + 12 test-source files)

java -cp /tmp/verify/out:<stdlib> verify.Runner <11 test classes>
```

Result (verbatim tail):

```
• LibraryGroupingTest (5 tests)
• RecentPlayedListTest (5 tests)
• SavedSessionCodecTest (4 tests)
• ThemeModeTest (2 tests)
• QueueEditorTest (12 tests)
• RepeatModeTest (5 tests)
• SearchIndexTest (9 tests)
• PermissionDecisionTest (8 tests)
• FormattersTest (4 tests)
• WindowSizeTest (2 tests)
• ThemeResolutionTest (2 tests)

TESTS PASSED: 58, FAILED: 0
```

Compiled files: `Models.kt`, `ThemeMode.kt`, `LibraryGrouping.kt`, `RecentPlayedList.kt`,
`SavedSession.kt`, `QueueEditor.kt`, `PlayerUiState.kt`, `AudioAccessState.kt`, `SearchIndex.kt`,
`Formatters.kt`, `SairenWindowSize.kt`, `ThemeResolution.kt` — plus their test classes. Because the
compiler is a *real* Kotlin compiler, this also type-checks every declaration those files use.

### 4. CI build and the two bugs it caught

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

After both fixes, run **`42212827921`** is green end to end: unit tests, lint, APK assembly, the APK
contents report and the artifact upload. Annotations from that run:

```
Unit tests   : Executed 71 tests in 14 classes, 0 failed, 0 skipped.
Lint         : passed
Assemble     : app-debug.apk (15,646 KiB)
lib/         : 4 files, arm64-v8a + armeabi-v7a only (see ABI notes)
```

No test was deleted, skipped, weakened or disabled to get there; the two changes are the resource fix
and the `RowScope` fix.

### 5. What is explicitly **not** verified

| Item | Status |
| --- | --- |
| `./gradlew testDebugUnitTest`, `lintDebug`, `assembleDebug` | **RUN — green in GitHub Actions** (run `42212827921`); still **not run locally**, because the sandbox has no Gradle distribution, Android SDK or Maven access |
| `assembleRelease` (R8 minify + resource shrinking) | **NOT RUN** — CI builds debug only |
| Debug APK existence and `unzip -l` `lib/` inspection | **RUN** — 15,646 KiB APK, 4 ARM-only native libraries, artifact uploaded |
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
