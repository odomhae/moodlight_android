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
- **Monetization**: Google Play Billing (one-time + subscription) + AdMob ads

---

## Build & Test

Requires **Java 17**. On Windows use `.\gradlew` (PowerShell) or `gradlew.bat`.

```bash
# Debug build
./gradlew assembleDebug

# Release build (minification disabled — no ProGuard config)
./gradlew assembleRelease

# Unit tests
./gradlew test

# Instrumentation tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Lint
./gradlew lint
```

> **Note**: The `test/` and `androidTest/` directories currently contain only the auto-generated `ExampleUnitTest` / `ExampleInstrumentedTest` placeholders — there are no real test suites yet.

Key dependency versions (from `gradle/libs.versions.toml`):

| Library | Version |
|---------|---------|
| AGP | 8.9.1 |
| Kotlin | 2.0.21 |
| Compose BOM | 2024.09.00 |
| Hilt | 2.51.1 |
| DataStore | 1.1.1 |
| Play Billing | 7.0.0 |
| Play Ads | 23.6.0 |

---

## Architecture

```
app/src/main/java/com/odom/moodlight/
├── MainActivity.kt                     # Single activity entry point
├── MoodLightApplication.kt             # Hilt application class
├── MoodLightDeviceAdminReceiver.kt     # Device admin for screen lock on timer end
├── data/
│   ├── SoundPlayer.kt                  # Singleton MediaPlayer manager
│   ├── RewardedAdManager.kt            # Rewarded ad flow → grants PRO via SharedPreferences
│   ├── datastore/
│   │   └── AppPreferences.kt           # DataStore keys and read/write helpers
│   ├── model/
│   │   ├── SoundType.kt                # Enum: sound assets, isPro flag
│   │   └── VisualPattern.kt            # Enum: NONE, STARLIGHT, CANDLE_FLICKER, WAVE, SNOWFALL
│   └── repository/
│       ├── BillingRepository.kt        # Google Play Billing (IAP)
│       └── SettingsRepository.kt       # Thin wrapper over AppPreferences
├── di/
│   ├── AppModule.kt
│   └── BillingModule.kt
├── service/
│   └── AudioService.kt                 # Foreground service for background audio
└── ui/
    ├── component/
    │   ├── AdBannerView.kt             # AdMob banner ad composable
    │   ├── BrightnessSlider.kt
    │   ├── ColorPaletteSheet.kt        # HSL color picker (hue/saturation/lightness sliders + GradientSlider)
    │   ├── ColorPickerRow.kt           # Preset chips + rainbow + add button + recent custom colors row
    │   ├── LightOrb.kt                 # Animated glowing orb (hidden for NONE/WAVE patterns)
    │   ├── PaywallBottomSheet.kt       # PRO upgrade bottom sheet
    │   ├── ProBadgeOverlay.kt
    │   ├── SoundCard.kt
    │   ├── SoundChip.kt
    │   ├── TimerArcProgress.kt
    │   ├── VisualPatternEffect.kt      # Canvas-based pattern renderers (Starfield, Candle, Wave, Snow)
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
    │   │   └── SoundViewModel.kt
    │   └── timer/
    │       ├── TimerScreen.kt          # Standalone timer screen (not in bottom nav)
    │       └── TimerViewModel.kt       # Self-contained countdown; end actions: CLOSE_APP, DIM_AND_CLOSE, PLAY_ALARM
    └── theme/
        ├── Color.kt                    # AppColors object
        ├── Theme.kt                    # MoodLightTheme (always dark)
        └── Type.kt
```

---

## Navigation

Three tabs defined in `AppNavigation.kt`:

| Tab | Route | Screen |
|-----|-------|--------|
| 💡 조명 | `light` | `LightScreen` |
| 🎵 사운드 | `sound` | `SoundScreen` |
| ⚙️ 설정 | `settings` | `SettingsScreen` |

- Back button triggers an exit confirmation `ModalBottomSheet` with an AdMob banner ad inside.
- Default start destination: `light`.
- An interstitial ad fires every 5 tab switches (tracked in `SharedPreferences` inside `AppNavigation.kt`).
- `ui/screen/timer/` exists but is **not wired into the bottom nav** — it is a standalone timer screen for future integration.

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

**Status bar**: Always uses light (white) icons via `SystemBarStyle.dark(Color.TRANSPARENT)` in `MainActivity.enableEdgeToEdge()`. Do not change this — the app background is always dark.

---

## Light Tab (`LightScreen` / `LightViewModel`)

### Key features
- Full-screen colored ambient light with animated `LightOrb` at center
- Screen is always kept on (`FLAG_KEEP_SCREEN_ON`) while the Light tab is active
- Brightness controlled via `WindowManager.LayoutParams.screenBrightness` reflected in state
- Sleep mode: screen fades to 10% brightness over 5 minutes; long-press to exit

### Opening the control bottom sheet
- **Swipe up** anywhere in the bottom 65% of the screen (≥ 80px upward drag)
- Visual hint at bottom: bouncing `KeyboardArrowUp` icon + "밀어서 색상·밝기 조절" text
- `ControlBottomSheet`: color picker + brightness slider (`skipPartiallyExpanded = true`)

### Timer
- Presets: 1, 15, 30, 60, 120, 180, 240, 300, 360 minutes (`TimerBottomSheet`)
- Timer tap button shown at top when no timer is running
- On timer end:
  1. `soundPlayer.stopAll()`
  2. If device admin is active → `DevicePolicyManager.lockNow()` then `finishAndRemoveTask()`
  3. Otherwise → dim screen to `screenBrightness = 0f`, wait 600 ms, then `finishAndRemoveTask()`
- `LightViewModel.exitApp: SharedFlow<Unit>` drives the exit sequence in `LightScreen`

### Visual patterns
Five patterns selectable in the control sheet (stored in `VisualPattern` enum):

| Pattern | Effect |
|---------|--------|
| `NONE` | Solid background, no overlay |
| `STARLIGHT` | Canvas particle field with twinkling stars |
| `CANDLE_FLICKER` | Flickering warm flame particles |
| `WAVE` | 4 layered sine waves (black-on-color shadow, alpha 0.22→0.07) |
| `SNOWFALL` | Falling snowflake particles |

- `LightOrb` is hidden for `NONE` and `WAVE` patterns.
- Patterns render in `VisualPatternEffect.kt` as Canvas composables with a 30%-diameter exclusion zone around center.
- `WAVE` background equals the selected color (not dark); the black sine waves create a shadow-flow effect.

### HSL color picker
- `ColorPaletteSheet.kt` contains an HSL picker launched from the `+` button in `ColorPickerRow`.
- Three `GradientSlider` composables (private) drive hue (0–360°), saturation (0–1), lightness (0–1).
- `GradientSlider` uses `awaitEachGesture → awaitPointerEvent → drag` for real-time color preview.
- Selected colors are saved as recent custom colors (max 5) in `LightViewModel`; deduplication threshold is ARGB difference > 100,000.

### Text contrast
- All overlay text uses perceived luminance: `R×0.299 + G×0.587 + B×0.114`.
- Formula is applied as `effectiveLuminance = perceivedLuminance × animatedBrightness`.
- Threshold 0.45: above → black text, below → white text.
- Brightness dimming is implemented as a black overlay (`Color.Black.copy(alpha = 1 - brightness)`) between the Orb layer and the text layer — the root Box does **not** use `.alpha()`, keeping text always 100% opaque.

### Center icon / emoji
- 5 emoji presets: 🌙 👶 🌟 🐑 🦋 (index stored in DataStore)
- Custom image from camera or gallery (stored as `custom_icon.jpg` in `filesDir`)
- EXIF rotation is corrected on save (`rotateBitmap()` in `SettingsScreen.kt`)
- `nextEmoji()` in `LightViewModel` cycles through presets and clears custom image

---

## Settings Tab (`SettingsScreen` / `SettingsViewModel`)

### Persisted settings (DataStore keys in `AppPreferences`)

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `color_index` | Int | 0 | Selected color index into `cycleColors` |
| `brightness` | Float | 0.8 | Screen brightness (0–1) |
| `emoji_index` | Int | 0 | Active emoji preset index |
| `custom_icon_path` | String | "" | Absolute path to custom icon JPEG |
| `icon_change_count` | Int | 0 | Cumulative icon change count (persisted across restarts) |
| `auto_restore` | Boolean | true | Always true — not shown in UI |
| `orientation` | String | "portrait" | Not shown in UI (reserved) |
| `language` | String | "ko" | Not shown in UI (reserved) |

### Interstitial ad trigger
- Every **3rd icon change** (emoji preset or custom image) triggers an interstitial ad
- Count is persisted in DataStore (`icon_change_count`) so it survives app restarts
- `SettingsRepository.incrementAndGetIconChangeCount()` atomically reads, increments, saves

### Timer screen-off permission
- "타이머 종료 시 화면 끄기" toggle — uses `DevicePolicyManager` device admin
- Receiver: `MoodLightDeviceAdminReceiver` (declared in `AndroidManifest.xml`)
- Policy file: `res/xml/device_admin.xml` (uses `force-lock` policy)
- Admin status refreshed on `ON_RESUME` via `LifecycleEventObserver`
- Enabling: launches `DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN` intent
- Disabling: calls `dpm.removeActiveAdmin(adminComponent)`

### Removed settings (intentionally)
- Screen orientation selector (hardcoded to portrait in manifest)
- Language selector
- Auto-restore toggle (always on)

---

## Sound Tab (`SoundScreen` / `SoundViewModel`)

- Free sounds: 🌧️ Rain, 🌊 Wave, 🌲 Forest
- PRO sounds: 🔥 Fire, 🎵 Lullaby, 🎹 Piano, 🌬️ Wind
- `SoundPlayer` (Hilt `@Singleton`) manages `MediaPlayer` instances keyed by `SoundType`
- Multiple sounds can be active simultaneously; each has an independent volume
- PRO gate: tapping a PRO sound when not PRO shows `PaywallBottomSheet`
- `AudioService` is a foreground service (`FOREGROUND_SERVICE_MEDIA_PLAYBACK`) that keeps audio running when the app is backgrounded; it binds to `SoundPlayer`

---

## Monetization

### AdMob
- Test App ID: `ca-app-pub-3940256099942544~3347511713`
- Test Interstitial ID: `ca-app-pub-3940256099942544/1033173712` (`INTERSTITIAL_AD_UNIT_ID` constant in `PaywallBottomSheet.kt`)
- Banner ad shown inside the back-press exit confirmation sheet
- Interstitial ad shown on every 3rd icon change

### Rewarded ads (alternative PRO unlock)
- `RewardedAdManager` loads and shows a rewarded ad; on reward callback it writes `is_pro = true` to `SharedPreferences`.
- `PaywallBottomSheet` shows either "Watch ad" or "loading…" depending on ad readiness.

### Google Play Billing
- Product IDs: `babylight_pro_lifetime`, `babylight_pro_monthly`
- `BillingRepository` manages connection, purchase flow, and persists PRO status in `SharedPreferences`
- `isPro: StateFlow<Boolean>` is observed by `LightViewModel` and `SettingsViewModel`

---

## Bottom Sheets

All bottom sheets use `ModalBottomSheet` (Material3). Pattern:

```kotlin
val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = AppColors.Panel
) { ... }
```

`skipPartiallyExpanded = true` is required on all sheets so a single downward swipe closes them directly.

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

### Device Admin
- `MoodLightDeviceAdminReceiver` is a minimal empty subclass of `DeviceAdminReceiver`
- Only the `force-lock` policy is declared — no other device admin capabilities
- Always check `dpm.isAdminActive(adminComponent)` before calling `lockNow()`
