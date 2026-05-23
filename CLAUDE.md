# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

# MoodLight — CLAUDE.md

## Project Overview

**MoodLight** is an Android baby night-light app built with Jetpack Compose. It provides colored ambient lighting, nature sounds, and a sleep timer. The target audience is parents using their phone as a nightlight for infants.

- **Package**: `com.odom.moodlight`
- **Min SDK**: 26 | **Target/Compile SDK**: 36
- **Language**: Kotlin (JVM target 17)
- **UI**: Jetpack Compose + Material3
- **DI**: Hilt
- **Persistence**: DataStore Preferences
- **Monetization**: AdMob ads (billing/IAP removed)

---

## Build & Test

Requires **Java 17**. On Windows use `.\gradlew` (PowerShell) or `gradlew.bat`.

```bash
# Debug build (R8 enabled)
./gradlew assembleDebug

# Release build (R8 enabled)
./gradlew assembleRelease

# Unit tests
./gradlew test

# Instrumentation tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Lint
./gradlew lint
```

> **Note**: The `test/` and `androidTest/` directories currently contain only the auto-generated `ExampleUnitTest` / `ExampleInstrumentedTest` placeholders — there are no real test suites yet.

**R8 / Minification**: Both `debug` and `release` build types have `isMinifyEnabled = true` and `isShrinkResources = true`. The only project-specific ProGuard rule is in `proguard-rules.pro`: keeping `SoundType` enum member names (they are stored as raw strings in DataStore via `SoundType.name`). All other libraries (Hilt, AdMob, DataStore, Coroutines, Compose) supply their own consumer rules via AAR.

Key dependency versions (from `gradle/libs.versions.toml`):

| Library | Version |
|---------|---------|
| AGP | 8.9.1 |
| Kotlin | 2.0.21 |
| Compose BOM | 2024.09.00 |
| Hilt | 2.51.1 |
| DataStore | 1.1.1 |
| Play Ads | 23.6.0 |

---

## Architecture

```
app/src/main/java/com/odom/moodlight/
├── MainActivity.kt                     # Single activity entry point
├── MoodLightApplication.kt             # Hilt application class + notification channel setup
├── MoodLightDeviceAdminReceiver.kt     # Device admin for screen lock on timer end
├── data/
│   ├── SoundPlayer.kt                  # Singleton MediaPlayer manager (separate players for lullaby vs white noise)
│   ├── datastore/
│   │   └── AppPreferences.kt           # DataStore keys and read/write helpers
│   ├── model/
│   │   ├── LullabyTrack.kt             # Data class: title + fileName, dynamically scanned from assets/lullaby/
│   │   ├── SoundType.kt                # Enum: white noise assets (RAIN, WAVE, FOREST, SHHH)
│   │   └── VisualPattern.kt            # Enum: NONE, STARLIGHT, CANDLE_FLICKER, WAVE, SNOWFALL, HEARTBEAT, BUBBLE_FLOAT
│   └── repository/
│       └── SettingsRepository.kt       # Thin wrapper over AppPreferences
├── service/
│   └── AudioService.kt                 # Foreground service for background audio
└── ui/
    ├── component/
    │   ├── AdBannerView.kt             # AdMob banner composable
    │   ├── BrightnessSlider.kt
    │   ├── ColorPaletteSheet.kt        # HSL color picker (hue/saturation/lightness sliders + GradientSlider)
    │   ├── ColorPickerRow.kt           # Preset chips + rainbow + add button + recent custom colors row
    │   ├── LightOrb.kt                 # Animated glowing orb (hidden for NONE/WAVE/HEARTBEAT patterns)
    │   ├── SoundCard.kt
    │   ├── SoundChip.kt
    │   ├── TimerArcProgress.kt
    │   ├── VisualPatternEffect.kt      # Canvas-based pattern renderers
    │   ├── WaveformAnimation.kt
    │   └── WheelPicker.kt
    ├── navigation/
    │   └── AppNavigation.kt            # NavHost + bottom nav + exit confirmation sheet
    ├── screen/
    │   ├── light/
    │   │   ├── LightScreen.kt          # Main lighting screen
    │   │   └── LightViewModel.kt
    │   ├── settings/
    │   │   ├── SettingsScreen.kt
    │   │   └── SettingsViewModel.kt
    │   ├── sound/
    │   │   ├── SoundScreen.kt
    │   │   └── SoundViewModel.kt       # Also defines SoundTab enum
    │   └── timer/
    │       ├── TimerScreen.kt          # Standalone timer screen (not in bottom nav)
    │       └── TimerViewModel.kt       # Self-contained countdown; end actions: CLOSE_APP, DIM_AND_CLOSE, PLAY_ALARM
    └── theme/
        ├── Color.kt                    # AppColors object
        ├── Theme.kt                    # MoodLightTheme (always dark)
        └── Type.kt
```

> **Removed files** (deleted): `data/RewardedAdManager.kt`, `data/repository/BillingRepository.kt`, `di/BillingModule.kt`, `ui/component/PaywallBottomSheet.kt`, `ui/component/ProBadgeOverlay.kt`. The PRO paywall and Google Play Billing have been fully stripped out.

---

## Navigation

Three tabs defined in `AppNavigation.kt`:

| Tab | Route | Screen |
|-----|-------|--------|
| 💡 조명 | `light` | `LightScreen` |
| 🎵 사운드 | `sound` | `SoundScreen` |
| ⚙️ 설정 | `settings` | `SettingsScreen` |

- `Screen` sealed class uses `@StringRes labelRes: Int` (not a hardcoded string) for tab labels.
- Back button triggers an exit confirmation `ModalBottomSheet` with an AdMob banner ad inside.
- Default start destination: `light`.
- An interstitial ad fires every 5 tab switches (tracked in `SharedPreferences` inside `AppNavigation.kt`).
- `ui/screen/timer/` exists but is **not wired into the bottom nav** — standalone timer screen for future integration.

---

## String Resources & Localization

All user-visible strings must be in `res/values/strings.xml` (Korean) and `res/values-en/strings.xml` (English).

- **In Composables**: use `stringResource(R.string.key)`
- **In non-Composable contexts** (Application, Service): use `context.getString(R.string.key)`

Do **not** use `R.string.key.toString()` — that converts the integer resource ID to a string, not the string value.

---

## Design System

All colors are in `AppColors` (`ui/theme/Color.kt`). The app is **always dark-themed** — do not add light mode support.

```kotlin
AppColors.Background  = 0xFF0D0A14   // Deep dark purple-black
AppColors.Panel       = 0xFF1A1625   // Slightly lighter panel
AppColors.Border      = 0x1AFFFFFF   // Subtle white border
AppColors.TextPrimary = 0xFFF0E8FF   // Near-white with warm tint
AppColors.TextDim     = 0x73F0E8FF   // 45% opacity text

// Accent colors (also used as lighting colors)
AppColors.WarmYellow  = 0xFFFFD6A0
AppColors.SkyBlue     = 0xFFA8D8FF
AppColors.MintGreen   = 0xFFB8F5C8
AppColors.SoftPink    = 0xFFFFB8D9
AppColors.Lavender    = 0xFFD4B8FF

AppColors.cycleColors = listOf(WarmYellow, SkyBlue, MintGreen, SoftPink, Lavender)
```

**Status bar**: Always uses light (white) icons via `SystemBarStyle.dark(Color.TRANSPARENT)` in `MainActivity.enableEdgeToEdge()`. Do not change this.

---

## Light Tab (`LightScreen` / `LightViewModel`)

### Key features
- Full-screen colored ambient light with animated `LightOrb` at center
- Screen is always kept on (`FLAG_KEEP_SCREEN_ON`) while the Light tab is active
- Brightness controlled via `WindowManager.LayoutParams.screenBrightness` reflected in state
- Sleep mode: screen fades to 5% brightness after 5 minutes; long-press to exit

### Opening the control bottom sheet
- **Swipe up** anywhere in the bottom 65% of the screen (≥ 80px upward drag)
- Visual hint at bottom: bouncing `KeyboardArrowUp` icon + hint text
- `ControlBottomSheet`: color picker + brightness slider (`skipPartiallyExpanded = true`)

### Timer
- Presets: 1, 15, 30, 60, 120, 180, 240, 300, 360 minutes (`TimerBottomSheet`)
- Timer tap button shown at top when no timer is running
- **Auto-restores on startup**: last timer minutes saved via `KEY_LAST_TIMER_MINUTES`; if > 0 on init, `startTimer()` is called immediately
- `startTimer()` also auto-plays the saved sound (`savedSoundMode` / `savedSoundName`)
- On timer end:
  1. `soundPlayer.stopAll()`
  2. If device admin is active → `DevicePolicyManager.lockNow()` then `finishAndRemoveTask()`
  3. Otherwise → dim screen to `screenBrightness = 0f`, wait 600 ms, then `finishAndRemoveTask()`
- `LightViewModel.exitApp: SharedFlow<Unit>` drives the exit sequence in `LightScreen`

### Background / foreground lifecycle
- `LightViewModel` observes `ProcessLifecycleOwner` to detect app backgrounding
- `ON_STOP`: pauses the timer job and stops all sounds; saves whether sound was active and timer was running
- `ON_START` (if `wasInBackground`): resumes the timer countdown from where it left off, restores sounds from saved state

### Sound button on Light tab
- `cycleSoundMode()` cycles: **off → white noise → lullaby → off**
- White noise restored to last-selected type (reads `savedSoundName`; falls back to `RAIN` if empty)
- `savedSoundName` is **always preserved** across all state transitions — it is never reset to `""` when stopping. This ensures the white noise selection survives tab switching, lullaby play, and app restart.
- 🎵 for lullaby, `SoundType.emoji` for white noise, 🔇 when off

### Visual patterns

| Pattern | Effect |
|---------|--------|
| `NONE` | Solid background, no overlay |
| `STARLIGHT` | Canvas particle field with twinkling stars |
| `CANDLE_FLICKER` | Flickering warm flame particles |
| `WAVE` | 4 layered sine waves (black-on-color shadow) |
| `SNOWFALL` | Falling snowflake particles |
| `HEARTBEAT` | Pulsing ♥ at center (replaces `LightOrb`); 600 ms cadence |
| `BUBBLE_FLOAT` | Floating bubble Canvas overlay |

- `LightOrb` is hidden for `NONE`, `WAVE`, and `HEARTBEAT` patterns.
- Patterns render in `VisualPatternEffect.kt` with a 30%-diameter exclusion zone around center.
- `WAVE` background equals the selected color; black sine waves create a shadow-flow effect.
- **Default pattern** (when stored value is `"none"` or empty): `STARLIGHT` — see `AppPreferences.visualPattern`.

### HSL color picker
- `ColorPaletteSheet.kt` — launched from the `+` button in `ColorPickerRow`
- Three `GradientSlider` composables drive hue (0–360°), saturation (0–1), lightness (0–1)
- `GradientSlider` uses `awaitEachGesture → awaitPointerEvent → drag` for real-time preview
- Recent custom colors: max 5, deduplication threshold is ARGB difference > 100,000
- Full ARGB persisted as `KEY_SELECTED_COLOR_ARGB` (Long); recent colors list as `KEY_RECENT_COLORS` (encoded String)

### Text contrast
- Perceived luminance: `R×0.299 + G×0.587 + B×0.114` × `animatedBrightness`
- Threshold 0.45: above → black text, below → white text
- Brightness dimming is a black overlay between the Orb layer and the text layer — the root Box does **not** use `.alpha()`, keeping text always 100% opaque

### Center icon / emoji
- 5 emoji presets: 🌙 👶 🌟 🐑 🦋 (index stored in DataStore)
- Custom image from camera or gallery (stored as `custom_icon.jpg` in `filesDir`)
- EXIF rotation corrected on save (`rotateBitmap()` in `SettingsScreen.kt`)

---

## Settings Tab (`SettingsScreen` / `SettingsViewModel`)

### Persisted settings (DataStore keys in `AppPreferences`)

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `color_index` | Int | 0 | Selected preset color index into `cycleColors` |
| `selected_color_argb` | Long | 0xFFFFD6A0 | Full ARGB of active color (preset or custom) |
| `recent_colors` | String | "" | Encoded list of up to 5 recent custom colors |
| `brightness` | Float | 0.8 | Screen brightness (0–1) |
| `visual_pattern` | String | → "starlight" | Active `VisualPattern.id`; "none"/empty maps to "starlight" |
| `emoji_index` | Int | 0 | Active emoji preset index |
| `custom_icon_path` | String | "" | Absolute path to custom icon JPEG |
| `icon_change_count` | Int | 0 | Cumulative icon change count for interstitial trigger |
| `last_timer_minutes` | Int | 0 | Last-set timer minutes; auto-restores timer on startup if > 0 |
| `saved_sound_mode` | String | "NONE" | "NONE" \| "LULLABY" \| "WHITE_NOISE" |
| `saved_sound_name` | String | "" | `SoundType.name` of last white noise; **never reset to ""** |

### Interstitial ad trigger
- Every **3rd icon change** (emoji preset or custom image) triggers an interstitial ad
- `SettingsRepository.incrementAndGetIconChangeCount()` atomically reads, increments, saves

### Timer screen-off permission
- Uses `DevicePolicyManager` device admin; receiver: `MoodLightDeviceAdminReceiver`
- Policy file: `res/xml/device_admin.xml` (`force-lock` policy only)
- Admin status refreshed on `ON_RESUME` via `LifecycleEventObserver`
- Always check `dpm.isAdminActive(adminComponent)` before calling `lockNow()`

---

## Sound Tab (`SoundScreen` / `SoundViewModel`)

Two sub-tabs: `SoundTab.LULLABY` and `SoundTab.WHITE_NOISE`. Sound selection is **mutually exclusive**.

### Lullaby sub-tab
- Tracks dynamically scanned from `assets/lullaby/` (`.mp3`, `.m4a`, sorted alphabetically)
- `playLullabyAtIndex()`: on completion plays `(index + 1) % size` → full **playlist loop**
- Auto-skips missing files

### White Noise sub-tab
- `SoundType` enum: `RAIN`, `WAVE`, `FOREST`, `SHHH` (4 entries; files in `assets/whiteSound/`)
- `playWhiteNoise()`: `isLooping = true` → **single track loop**
- **Asset note**: `assets/whiteSound/` may contain extra files not wired to any `SoundType`. To add a new sound: add the file **and** a matching `SoundType` enum entry.
- `SoundType` enum member names are stored as strings in DataStore — **do not rename enum constants** without updating existing saved data. The ProGuard rule `-keepnames class SoundType` preserves this.

### AudioService
- Foreground service (`FOREGROUND_SERVICE_MEDIA_PLAYBACK`), started/stopped via `Intent(ACTION_START/STOP)`
- Notification title/content use `getString(R.string.notification_title/content)`

---

## Monetization

### AdMob
Ad unit IDs live in `res/values/strings.xml` (both TEST and REAL IDs):
- `R.string.REAL_ADMOB_BANNER_ID` / `R.string.TEST_ADMOB_BANNER_ID`
- `R.string.REAL_ADMOB_INTERSTITIAL_ID` / `R.string.TEST_ADMOB_INTERSTITIAL_ID`

`AdBannerView` loads the banner with `context.getString(R.string.REAL_ADMOB_BANNER_ID)`. `AppNavigation` loads the interstitial with `stringResource(R.string.REAL_ADMOB_INTERSTITIAL_ID)`.

Ad placements:
- Banner: inside the back-press exit confirmation sheet
- Interstitial: every 5 tab switches (`AppNavigation.kt`) and every 3rd icon change (`SettingsScreen`)

> Google Play Billing and rewarded-ad PRO unlock have been fully removed.

### In-app Review (Play Core)
Triggered in two places: `LightScreen` (on timer start confirm) and `SettingsScreen` (on "리뷰 남기기" tap).

---

## Bottom Sheets

All bottom sheets use `ModalBottomSheet` (Material3):

```kotlin
val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = AppColors.Panel
) { ... }
```

`skipPartiallyExpanded = true` is required on all sheets.

---

## Key Patterns & Conventions

### ViewModel state
- All screen state in a single `data class UiState` exposed as `StateFlow`
- Side effects (exit, show ad) via `MutableSharedFlow` with `extraBufferCapacity = 1`
- Collected in `LaunchedEffect(Unit)` on the screen composable

### DataStore access
- All keys in `AppPreferences.companion object`
- Repository is a thin pass-through; business logic stays in ViewModel
- One-shot reads use `store.data.first()[KEY]`

### Image handling
- Custom icon always saved as `filesDir/custom_icon.jpg`
- EXIF orientation corrected before saving using `android.media.ExifInterface`
- `rotateBitmap()` private top-level function in `SettingsScreen.kt` handles all 7 EXIF orientations
