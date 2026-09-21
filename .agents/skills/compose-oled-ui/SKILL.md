---
name: compose-oled-ui
description: >-
  Expert guidelines and workflows for developing high-performance, battery-saving Jetpack Compose UI
  specifically optimized for true pitch-black OLED screens (#000000), outdoor sunlight readability,
  and dynamic typography. Use whenever designing, modifying, or testing bike computer UI, dark-mode dashboards,
  zero-border styling, or variable fonts in Android.
---

# Jetpack Compose OLED & Cockpit UI Skill

This skill documents proven best practices, architectural patterns, and validation runbooks for building lightweight, battery-efficient Android bike computer and OLED-centric cockpit interfaces.

---

## 1. Zero-Power OLED Design Rules

To guarantee 0mA current draw on OLED / AMOLED subpixels:
1. **True Pitch Black Background (`#000000`)**:
   - Never use dark greys like `#121212` for primary dashboard canvas surfaces. Set `Modifier.background(Color(0xFF000000))`.
2. **Strict Zero-Border Policy**:
   - Never add 1px/2px border strokes or outlines to buttons, cards, or pills (`border: none`).
   - Use tonal surface differentiation (e.g. `#1E1E20` for pills, `#27272B` for primary controls, `#141416` for settings cards).
3. **Contrast Hierarchy for Direct Sunlight**:
   - **Critical Metric Values (Speed, Distance, HR)**: White `#FFFFFF` or high-intensity amber `#F97316` / emerald `#4ADE80`.
   - **Labels and Units**: Neutral grey `#A3A3A3` at 11sp–14sp.

---

## 2. Dynamic Variable Typography (Roboto Flex)

When using continuous parametric variable fonts (such as Google Fonts' **Roboto Flex**):
1. **Dynamic Condensation vs Width Expansion**:
   - Single-digit speed values (e.g., `8.5`): Wider stance (`wdth = 75`, `fontSize = 175.sp`).
   - Two-digit cruising speed (e.g., `33.3`): Condensed stance (`wdth = 38`, `fontSize = 170.sp`).
   - Three-digit speeds (e.g., `104.2`): Ultra-condensed (`wdth = 26`, `fontSize = 145.sp`) to prevent line wrapping without shrinking the visual height.
2. **Font Family Allocation Caching**:
   - Creating `FontFamily(Font(..., variationSettings))` inside composables on every 1Hz redraw causes continuous object allocation and Garbage Collection (GC) pauses.
   - **Rule**: Always cache variations using a memoized lookup table:
     ```kotlin
     private val fontCache = mutableMapOf<Pair<Float, Int>, FontFamily>()
     fun getRobotoFlexFont(width: Float, weight: Int): FontFamily =
         fontCache.getOrPut(Pair(width, weight)) { ... }
     ```

---

## 3. Battery-Preserving Lifecycle & State Invalidation

1. **Throttled 1Hz Telemetry Pipeline**:
   - Even if sensors provide high-frequency raw data (e.g. 10Hz GPS or 20Hz accelerometers), downsample UI emissions to **1 Hz** via `combine` and `throttleFirst`/`sample(1000)`.
2. **Lifecycle-Aware Flow Collection**:
   - Always use `collectAsStateWithLifecycle()` from `androidx.lifecycle:lifecycle-runtime-compose` instead of standard `collectAsState()`. This stops background collection when the screen turns off.
3. **Disposable Window Flags**:
   - When managing `FLAG_KEEP_SCREEN_ON`, wrap it in a `DisposableEffect` to avoid triggering window mutations on every recomposition.

---

## 4. On-Device Emulator Verification Workflow

Run the following commands to verify UI state and layout stability on the Android emulator:

1. **Install and Launch App**:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   adb shell am start -n <package>/<activity>
   ```

2. **Capture and Inspect Pixel-Perfect Screenshot**:
   ```bash
   adb exec-out screencap -p > /tmp/screen_capture.png
   ```

3. **Simulate User Gestures**:
   - Swipe left to settings: `adb shell input swipe 900 1200 100 1200 250`
   - Tap buttons: `adb shell input tap <X> <Y>`
