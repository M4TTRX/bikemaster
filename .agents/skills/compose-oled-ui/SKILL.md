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

## 4. Zero-Border Modal Dialogs & Sheets

1. **True OLED Dialog Surface**:
   - Set `containerColor = Color(0xFF141416)` or `#1E1E20` for high contrast against `#000000`.
   - Set `tonalElevation = 0.dp` to prevent Material 3 from applying unwanted tinting.
   - Do not apply `border(...)` modifier under any circumstance.
2. **Action Affordances**:
   - Primary actions (e.g. Connect): Full accent color (e.g. `#FF4081` Pink) with dark text (`#000000`).
   - Secondary / Dismiss actions: Low-opacity circular touch targets (e.g. `IconButton` with close icon).

---

## 5. On-Device Emulator & UI Verification Workflow

When running UI automation and verification on Android emulators:

1. **Install and Launch App**:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   adb shell am start -n com.velo.app/.MainActivity
   ```

2. **Reliable Pixel-Perfect Screencap (Preventing Stream Stalls)**:
   - Avoid shell pipe redirection (`adb exec-out screencap -p > ...`) in subshells as stdout buffering can stall indefinitely.
   - Use Python binary pipe execution:
     ```python
     import subprocess
     with open('/path/to/capture.png', 'wb') as f:
         subprocess.run(['adb', 'exec-out', 'screencap', '-p'], stdout=f, check=True)
     ```

3. **Gesture Navigation vs Pager Swipe Tuning**:
   - Gestures starting near screen edges ($X > 930$ or $X < 70$ on a 1080px display) trigger Android system back/navigation gestures.
   - Swipes with duration $< 200\text{ms}$ risk being registered as taps or flings before drag slop threshold is met.
   - **Recommended Pager Swipe**:
     ```bash
     adb shell input swipe 800 1200 100 1200 350
     ```

4. **UI Inspection via UIAutomator Dump**:
   - Retrieve screen hierarchy and exact interactive node bounds:
     ```bash
     adb shell uiautomator dump /sdcard/window_dump.xml
     adb exec-out cat /sdcard/window_dump.xml
     ```
   - Tap center of bounds $[x_1, y_1][x_2, y_2]$: $((x_1 + x_2) / 2, (y_1 + y_2) / 2)$.

