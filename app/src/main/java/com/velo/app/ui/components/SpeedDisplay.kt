package com.velo.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velo.app.model.UnitSystem
import com.velo.app.ui.theme.getRobotoFlexFont
import java.util.Locale

@Composable
fun SpeedDisplay(
    speedValue: Float,
    unitSystem: UnitSystem,
    modifier: Modifier = Modifier
) {
    val displaySpeed = if (unitSystem == UnitSystem.METRIC) speedValue else speedValue * 0.621371f
    val formattedSpeed = String.format(Locale.US, "%.1f", displaySpeed)
    val integerPart = formattedSpeed.substringBefore(".")
    val numDigits = integerPart.length

    val (fontSize, fontWidth) = when {
        numDigits <= 1 -> 175.sp to 75f   // e.g. "8.5" -> massive, wider stance
        numDigits == 2 -> 170.sp to 38f   // e.g. "33.3" -> tall, condensed stance
        else -> 145.sp to 26f            // e.g. "104.2" -> ultra-condensed to fit screen without wrapping
    }

    val unitText = if (unitSystem == UnitSystem.METRIC) "km / h" else "mph"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 180.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Colossal Adaptive Speed Value
        Text(
            text = formattedSpeed,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = fontSize,
            fontFamily = getRobotoFlexFont(width = fontWidth, weight = 320),
            lineHeight = fontSize,
            maxLines = 1
        )

        // Wide letter-spaced unit
        Text(
            text = unitText,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            fontSize = 24.sp,
            fontFamily = getRobotoFlexFont(width = 135f, weight = 300),
            letterSpacing = 4.sp
        )
    }
}
