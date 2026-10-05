package com.velo.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.TextUnit
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

    // Milestone 10 km/h boundary detection (e.g. 9 to 10, or crossing any decade)
    val currentTens = (displaySpeed / 10).toInt()
    var previousTens by remember { mutableStateOf<Int?>(null) }
    val milestoneBounceScale = remember { Animatable(1f) }

    LaunchedEffect(currentTens) {
        if (previousTens != null && previousTens != currentTens) {
            // Kinetic squash & pop bounce for the speed milestone
            milestoneBounceScale.animateTo(
                targetValue = 1.15f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
            milestoneBounceScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        previousTens = currentTens
    }

    val decimalDigit = formattedSpeed.substringAfter(".", "0").firstOrNull() ?: '0'
    val unitsDigit = integerPart.lastOrNull() ?: '0'
    val tensDigit = integerPart.getOrNull(integerPart.length - 2)
    val hundredsDigit = integerPart.getOrNull(integerPart.length - 3)

    var lastTensDigit by remember { mutableStateOf('1') }
    if (tensDigit != null) {
        lastTensDigit = tensDigit
    }

    var lastHundredsDigit by remember { mutableStateOf('1') }
    if (hundredsDigit != null) {
        lastHundredsDigit = hundredsDigit
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 180.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Colossal Adaptive Speed Value with Milestone Bounce
        Row(
            modifier = Modifier.graphicsLayer {
                scaleX = milestoneBounceScale.value
                scaleY = milestoneBounceScale.value
            },
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Hundreds digit (visible for >= 100)
            AnimatedVisibility(
                visible = hundredsDigit != null,
                enter = fadeIn() + scaleIn(
                    initialScale = 0.7f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) + expandHorizontally(),
                exit = fadeOut() + scaleOut(targetScale = 0.8f) + shrinkHorizontally()
            ) {
                AnimatedDigit(
                    digit = hundredsDigit ?: lastHundredsDigit,
                    fontSize = fontSize,
                    fontWidth = fontWidth,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Tens digit (visible for >= 10)
            AnimatedVisibility(
                visible = tensDigit != null,
                enter = fadeIn() + scaleIn(
                    initialScale = 0.7f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) + expandHorizontally(),
                exit = fadeOut() + scaleOut(targetScale = 0.8f) + shrinkHorizontally()
            ) {
                AnimatedDigit(
                    digit = tensDigit ?: lastTensDigit,
                    fontSize = fontSize,
                    fontWidth = fontWidth,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Units digit (always visible)
            AnimatedDigit(
                digit = unitsDigit,
                fontSize = fontSize,
                fontWidth = fontWidth,
                color = MaterialTheme.colorScheme.onBackground
            )

            // Static decimal separator
            Text(
                text = ".",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = fontSize,
                fontFamily = getRobotoFlexFont(width = fontWidth, weight = 320),
                lineHeight = fontSize,
                maxLines = 1
            )

            // Tenths digit (always visible)
            AnimatedDigit(
                digit = decimalDigit,
                fontSize = fontSize,
                fontWidth = fontWidth,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

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

@Composable
private fun AnimatedDigit(
    digit: Char,
    fontSize: TextUnit,
    fontWidth: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = digit,
        transitionSpec = {
            val isIncreasing = targetState > initialState
            if (isIncreasing) {
                (slideInVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    initialOffsetY = { it / 2 }
                ) + fadeIn() + scaleIn(
                    initialScale = 0.72f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )).togetherWith(
                    slideOutVertically(
                        animationSpec = spring(stiffness = Spring.StiffnessMedium),
                        targetOffsetY = { -it / 2 }
                    ) + fadeOut() + scaleOut(targetScale = 0.82f)
                )
            } else {
                (slideInVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    initialOffsetY = { -it / 2 }
                ) + fadeIn() + scaleIn(
                    initialScale = 0.72f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )).togetherWith(
                    slideOutVertically(
                        animationSpec = spring(stiffness = Spring.StiffnessMedium),
                        targetOffsetY = { it / 2 }
                    ) + fadeOut() + scaleOut(targetScale = 0.82f)
                )
            }
        },
        contentAlignment = Alignment.Center,
        label = "SpeedDigitAnimation",
        modifier = modifier
    ) { char ->
        Text(
            text = char.toString(),
            color = color,
            fontSize = fontSize,
            fontFamily = getRobotoFlexFont(width = fontWidth, weight = 320),
            lineHeight = fontSize,
            maxLines = 1
        )
    }
}

