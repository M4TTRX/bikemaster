package com.velo.app.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.velo.app.R

val DarkColorScheme = darkColorScheme(
    primary = BtnNeutralGrey,
    onPrimary = BtnNeutralText,
    secondary = BtnFinishPink,
    onSecondary = BtnFinishTextDark,
    background = OledBlack,
    surface = OledBlack,
    onBackground = MetricValueWhite,
    onSurface = MetricValueWhite
)

val RobotoFlexFontFamily = FontFamily(
    Font(R.font.roboto_flex)
)

/**
 * Helper to dynamically create a Roboto Flex font style with continuous variable axes:
 * - wdth: width axis (25 to 151)
 * - wght: weight axis (100 to 1000)
 */
@OptIn(ExperimentalTextApi::class)
fun getRobotoFlexFont(width: Float = 100f, weight: Int = 400): FontFamily {
    return FontFamily(
        Font(
            resId = R.font.roboto_flex,
            variationSettings = FontVariation.Settings(
                FontVariation.width(width),
                FontVariation.weight(weight)
            )
        )
    )
}

@Composable
fun VeloTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
