package com.velo.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velo.app.model.RideStatus
import com.velo.app.ui.theme.getRobotoFlexFont

@Composable
fun BottomActionControls(
    rideStatus: RideStatus,
    onStartClick: () -> Unit,
    onPauseClick: () -> Unit,
    onResumeClick: () -> Unit,
    onFinishClick: () -> Unit,
    modifier: Modifier = Modifier,
    onResetClick: () -> Unit = {}
) {
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer
    val secondaryContainer = MaterialTheme.colorScheme.secondaryContainer
    val onSecondaryContainer = MaterialTheme.colorScheme.onSecondaryContainer
    val errorContainer = MaterialTheme.colorScheme.errorContainer
    val onErrorContainer = MaterialTheme.colorScheme.onErrorContainer

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
    ) {
        when (rideStatus) {
            RideStatus.IDLE -> {
                // STATE 1: IDLE - Faint Tonal Primary Container Start Ride Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .clip(CircleShape)
                        .background(primaryContainer)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(color = onPrimaryContainer),
                            onClick = onStartClick
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Start Ride",
                            tint = onPrimaryContainer,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Start Ride",
                            color = onPrimaryContainer,
                            fontSize = 18.sp,
                            fontFamily = getRobotoFlexFont(width = 100f, weight = 800)
                        )
                    }
                }
            }

            RideStatus.RECORDING -> {
                // STATE 2: Active Ride - Tonal Secondary Container Pause Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .clip(CircleShape)
                        .background(secondaryContainer)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(color = onSecondaryContainer),
                            onClick = onPauseClick
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Pause,
                            contentDescription = "Pause Ride",
                            tint = onSecondaryContainer,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Pause",
                            color = onSecondaryContainer,
                            fontSize = 17.sp,
                            fontFamily = getRobotoFlexFont(width = 100f, weight = 600)
                        )
                    }
                }
            }

            RideStatus.PAUSED -> {
                // STATE 3: Paused Ride - Faint Error Container Finish + Secondary Container Resume
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Finish Button (Faint Tonal Error Container with Square Icon)
                    Box(
                        modifier = Modifier
                            .height(58.dp)
                            .clip(CircleShape)
                            .background(errorContainer)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(color = onErrorContainer),
                                onClick = onFinishClick
                            )
                            .padding(horizontal = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Finish Ride",
                                tint = onErrorContainer,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Finish",
                                color = onErrorContainer,
                                fontSize = 16.sp,
                                fontFamily = getRobotoFlexFont(width = 100f, weight = 800)
                            )
                        }
                    }

                    // Resume Button (Secondary Container, Faint Tone)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(58.dp)
                            .clip(CircleShape)
                            .background(secondaryContainer)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(color = onSecondaryContainer),
                                onClick = onResumeClick
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Resume Ride",
                                tint = onSecondaryContainer,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Resume",
                                color = onSecondaryContainer,
                                fontSize = 17.sp,
                                fontFamily = getRobotoFlexFont(width = 100f, weight = 600)
                            )
                        }
                    }
                }
            }

            RideStatus.FINISHED -> {
                // STATE 4: Finished Ride - Reset / New Ride Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .clip(CircleShape)
                        .background(primaryContainer)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(color = onPrimaryContainer),
                            onClick = onResetClick
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "New Ride",
                            tint = onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "New Ride",
                            color = onPrimaryContainer,
                            fontSize = 18.sp,
                            fontFamily = getRobotoFlexFont(width = 100f, weight = 800)
                        )
                    }
                }
            }
        }
    }
}
