# MoodLight (Baby Night Light)

A soothing Android application designed to help babies and adults sleep better with customizable light and sound environments.

## ✨ Features

- **Customizable Night Light**:
    - Smooth color transitions and brightness control.
    - Multiple preset emojis and support for custom icons/images.
    - Long-press to lock/unlock sleep mode to prevent accidental touches.
- **Soothing Sounds**:
    - High-quality nature sounds: Rain, Waves, Forest, Fireplace, Wind.
    - Relaxing Lullabies and Piano melodies.
    - Sound Mixer to combine multiple sounds (PRO feature).
- **Smart Timer**:
    - Set precise durations with a wheel picker or quick presets.
    - Configurable end actions: Close app, Dim light, or Play alarm.
    - Optional Device Admin permission to automatically turn off the screen.
- **Premium (PRO) Version**:
    - Unlock all sound types.

## 🛠 Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM (Model-View-ViewModel)
- **Dependency Injection**: Hilt
- **Asynchronous Programming**: Coroutines & Flow
- **Data Layer**: Clean Repository pattern
- **Monetization**: Google Play Billing (In-App Purchases) & AdMob (Interstitial Ads)

## 📁 Project Structure

- `ui/`: Compose screens, components, and theme.
- `data/`: Repositories, Models, and Local/Remote data sources.
- `di/`: Hilt modules for dependency injection.

## 🌍 Localization

- Supported Languages: **Korean**, **English**.
- All UI strings are managed via `strings.xml` for easy translation.

## 🚀 Getting Started

1. Clone the repository.
2. Open the project in Android Studio (Ladybug or newer).
3. Build and run the `:app` module.

---
© 2024 MoodLight Team
