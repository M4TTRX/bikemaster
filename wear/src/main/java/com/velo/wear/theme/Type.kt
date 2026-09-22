package com.velo.wear.theme

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import com.velo.wear.R
import java.util.concurrent.ConcurrentHashMap

val RobotoFlexFontFamily = FontFamily(
    Font(R.font.roboto_flex)
)

private val fontCache = ConcurrentHashMap<Pair<Float, Int>, FontFamily>()

/**
 * Wear OS 6 standard variable font helper.
 * Dynamically creates and caches Roboto Flex instances with variable axes:
 * - wdth: width axis (25 to 151)
 * - wght: weight axis (100 to 1000)
 */
@OptIn(ExperimentalTextApi::class)
fun getRobotoFlexFont(width: Float = 100f, weight: Int = 400): FontFamily {
    val key = Pair(width, weight)
    return fontCache.computeIfAbsent(key) {
        FontFamily(
            Font(
                resId = R.font.roboto_flex,
                variationSettings = FontVariation.Settings(
                    FontVariation.width(width),
                    FontVariation.weight(weight)
                )
            )
        )
    }
}
