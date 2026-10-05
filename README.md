# BikeMaster 🚴‍♂️

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-purple.svg)](https://kotlinlang.org)
[![Platform](https://img.shields.io/badge/Platform-Android%20%7C%20Wear%20OS-green.svg)](https://developer.android.com)

**BikeMaster** is an open-source, high-performance cycling computer and telemetry tracking app for Android and Wear OS. Designed from the ground up for outdoor readability, battery efficiency, and seamless Bluetooth sensor integration.

---

## ✨ Features

- **Sunlight-Readable OLED Dashboard**: Built with Jetpack Compose featuring pure `#000000` pitch-black backgrounds, high-contrast dynamic typography, and zero-border design for maximum outdoor legibility and minimal battery draw.
- **Customizable Metric Tiles**: Drag, configure, and inspect primary and secondary metrics on the fly (Speed, Cadence, Heart Rate, Power, Distance, Ride Time, Average Speed).
- **Multi-Sensor BLE Connectivity**:
  - Cycling Speed and Cadence (CSC) sensors
  - Heart Rate Monitors (HRM / BLE GATT standard)
  - Cycling Power Meters (CPS)
  - Automatic reconnection and hardware persistence via AndroidX DataStore.
- **Wear OS Companion Integration**:
  - Wrist-based optical heart rate streaming powered by Android Health Services.
  - Bidirectional communication via Wearable Data Layer API.
  - Automatic companion app launch and ride sync when a session starts.
- **Battery-Safe Tracking**:
  - Foreground service (`RideTrackingService`) ensures reliable telemetry recording without Android OS process killing when the screen turns off.
- **Theme Flexibility**: Full support for True Pitch Black (OLED), System Dark, and Day Light modes.

---

## 📱 Architecture & Tech Stack

BikeMaster is organized as a multi-module Gradle project:

```text
bikemaster/
├── app/               # Android mobile application (Jetpack Compose, BLE manager, Foreground Service)
├── wear/              # Wear OS companion app (Compose for Wear OS, Health Services HR streaming)
└── gradle/            # Gradle build scripts & version catalogs
```

- **Language**: [Kotlin](https://kotlinlang.org)
- **UI Toolkit**: [Jetpack Compose](https://developer.android.com/jetpack/compose) & [Compose for Wear OS](https://developer.android.com/training/wearables/compose)
- **Bluetooth Stack**: Standard Android BLE GATT client + Nordic Semiconductor Android-BLE-Library
- **State & Concurrency**: Kotlin Coroutines, `StateFlow`, `SharedFlow`
- **Architecture**: Clean Architecture / MVVM (`ViewModel`, `Repository`, `Service`)
- **Persistence**: Jetpack DataStore (Preferences)
- **Wearable Communication**: Google Play Services Wearable Data Layer API

---

## 🛠️ Getting Started

### Prerequisites

- **Android Studio**: Ladybug (2024.2.1) or newer recommended
- **JDK**: Java 17 or higher
- **Android SDK**:
  - `minSdk`: 26 (Android 8.0 Oreo)
  - `targetSdk` / `compileSdk`: 34 (Android 14)

### Building the Project

Clone the repository and build using Gradle:

```bash
git clone https://github.com/<owner>/bikemaster.git
cd bikemaster

# Build debug APKs for both handheld and wear
./gradlew assembleDebug

# Run unit tests
./gradlew test
```

### Running on Device

1. Connect your Android phone and Wear OS watch via USB or Wireless ADB.
2. Ensure Bluetooth and Location permissions are enabled on your test device for BLE scanning.
3. Select the `app` or `wear` configuration in Android Studio and press **Run**.

---

## 🤝 Contributing

Contributions are welcome! Please check out [CONTRIBUTING.md](CONTRIBUTING.md) to get started with:
- Reporting bugs or feature requests
- Coding style guidelines and conventions
- Submitting pull requests

---

## 📄 License

This project is licensed under the Apache License, Version 2.0. See the [LICENSE](LICENSE) file for details.
