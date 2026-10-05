package com.velo.app.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velo.app.ui.theme.HrZone2Aerobic
import com.velo.app.ui.theme.getRobotoFlexFont
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun TopConnectionPills(
    isGpsActive: Boolean,
    isHeartRateConnected: Boolean,
    modifier: Modifier = Modifier,
    isBleCadenceConnected: Boolean = false,
    isBleSpeedConnected: Boolean = false,
    isBlePowerConnected: Boolean = false
) {
    val activeColor = HrZone2Aerobic // High-contrast emerald green for connected state

    var gpsProminent by remember { mutableStateOf(false) }
    var watchProminent by remember { mutableStateOf(false) }

    // Track previous states to trigger prominent animation only upon actual connection transition
    var prevGpsState by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var prevWatchState by rememberSaveable { mutableStateOf<Boolean?>(null) }

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(isGpsActive) {
        if (isGpsActive && prevGpsState != true) {
            gpsProminent = true
            delay(2500)
            gpsProminent = false
        } else if (!isGpsActive) {
            gpsProminent = false
        }
        prevGpsState = isGpsActive
    }

    LaunchedEffect(isHeartRateConnected) {
        if (isHeartRateConnected && prevWatchState != true) {
            watchProminent = true
            delay(2500)
            watchProminent = false
        } else if (!isHeartRateConnected) {
            watchProminent = false
        }
        prevWatchState = isHeartRateConnected
    }

    val isCscConnected = isBleCadenceConnected || isBleSpeedConnected
    val isPowerConnected = isBlePowerConnected

    // Calculate collapsed slot indices (packed from top-right to left)
    val gpsSlot = 0
    val watchSlot = if (isGpsActive) 1 else 0
    val cscSlot = (if (isGpsActive) 1 else 0) + (if (isHeartRateConnected) 1 else 0)
    val powerSlot = cscSlot + (if (isCscConnected) 1 else 0)

    val targetHeight = if (gpsProminent && watchProminent) 76.dp else 36.dp
    val animatedHeight by animateDpAsState(
        targetValue = targetHeight,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "containerHeight"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(animatedHeight)
    ) {
        val screenWidth = maxWidth

        // 1. GPS Pill (Prominent when connected, animates to collapsed icon in top-right)
        AnimatedConnectionPill(
            isConnected = isGpsActive,
            isProminent = gpsProminent && isGpsActive,
            title = "GPS Connected",
            icon = Icons.Default.LocationOn,
            slotIndex = gpsSlot,
            maxWidth = screenWidth,
            prominentY = 0.dp,
            prominentWidth = 155.dp,
            activeColor = activeColor,
            onTap = {
                coroutineScope.launch {
                    gpsProminent = true
                    delay(2500)
                    gpsProminent = false
                }
            }
        )

        // 2. Watch Pill (Prominent when connected, animates to collapsed icon in top-right)
        AnimatedConnectionPill(
            isConnected = isHeartRateConnected,
            isProminent = watchProminent && isHeartRateConnected,
            title = "Watch Connected",
            icon = Icons.Default.Watch,
            slotIndex = watchSlot,
            maxWidth = screenWidth,
            prominentY = if (gpsProminent && isGpsActive) 40.dp else 0.dp,
            prominentWidth = 170.dp,
            activeColor = activeColor,
            onTap = {
                coroutineScope.launch {
                    watchProminent = true
                    delay(2500)
                    watchProminent = false
                }
            }
        )

        // 3. Cadence / Speed sensor collapsed icon (if connected)
        if (isCscConnected) {
            val label = when {
                isBleSpeedConnected && isBleCadenceConnected -> "Speed+Cad"
                isBleSpeedConnected -> "Speed"
                else -> "Cadence"
            }
            CollapsedSensorIcon(
                icon = Icons.AutoMirrored.Filled.DirectionsBike,
                contentDescription = label,
                slotIndex = cscSlot,
                maxWidth = screenWidth,
                activeColor = activeColor
            )
        }

        // 4. Power meter collapsed icon (if connected)
        if (isPowerConnected) {
            CollapsedSensorIcon(
                icon = Icons.Default.Bolt,
                contentDescription = "Power Meter",
                slotIndex = powerSlot,
                maxWidth = screenWidth,
                activeColor = activeColor
            )
        }
    }
}

@Composable
private fun AnimatedConnectionPill(
    isConnected: Boolean,
    isProminent: Boolean,
    title: String,
    icon: ImageVector,
    slotIndex: Int,
    maxWidth: Dp,
    prominentY: Dp,
    prominentWidth: Dp,
    activeColor: Color,
    onTap: () -> Unit
) {
    val targetX = if (isProminent) {
        (maxWidth - prominentWidth) / 2
    } else {
        maxWidth - 34.dp - (slotIndex * 40).dp
    }

    val targetY = if (isProminent) prominentY else 0.dp
    val targetWidth = if (isProminent) prominentWidth else 34.dp
    val targetAlpha = if (isConnected) 1f else 0f

    val animX by animateDpAsState(
        targetValue = targetX,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "pillX_$title"
    )

    val animY by animateDpAsState(
        targetValue = targetY,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "pillY_$title"
    )

    val animWidth by animateDpAsState(
        targetValue = targetWidth,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "pillWidth_$title"
    )

    val animAlpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(durationMillis = 250),
        label = "pillAlpha_$title"
    )

    val textAlpha by animateFloatAsState(
        targetValue = if (isProminent) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "pillTextAlpha_$title"
    )

    if (animAlpha > 0.01f) {
        Box(
            modifier = Modifier
                .offset(x = animX, y = animY)
                .size(width = animWidth, height = 34.dp)
                .alpha(animAlpha)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .clickable(enabled = isConnected && !isProminent) {
                    onTap()
                },
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = activeColor,
                    modifier = Modifier.size(16.dp)
                )
                if (textAlpha > 0.05f) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = title,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.sp,
                        fontFamily = getRobotoFlexFont(width = 85f, weight = 600),
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                        modifier = Modifier.alpha(textAlpha)
                    )
                }
            }
        }
    }
}

@Composable
private fun CollapsedSensorIcon(
    icon: ImageVector,
    contentDescription: String,
    slotIndex: Int,
    maxWidth: Dp,
    activeColor: Color
) {
    val targetX = maxWidth - 34.dp - (slotIndex * 40).dp
    val animX by animateDpAsState(
        targetValue = targetX,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "sensorX_$contentDescription"
    )

    Box(
        modifier = Modifier
            .offset(x = animX, y = 0.dp)
            .size(34.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainer),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = activeColor,
            modifier = Modifier.size(16.dp)
        )
    }
}
