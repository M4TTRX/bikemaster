package com.velo.app.ui.util

import android.view.HapticFeedbackConstants
import android.view.View

/**
 * Utility extension to trigger crisp tactile click haptic feedback on button presses.
 * Uses KEYBOARD_TAP / VIRTUAL_KEY with FLAG_IGNORE_GLOBAL_SETTING to ensure
 * reliable haptic perception on Android devices.
 */
fun View.performAppClickHaptic() {
    val performed = performHapticFeedback(
        HapticFeedbackConstants.KEYBOARD_TAP,
        HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
    )
    if (!performed) {
        performHapticFeedback(
            HapticFeedbackConstants.VIRTUAL_KEY,
            HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
        )
    }
}
