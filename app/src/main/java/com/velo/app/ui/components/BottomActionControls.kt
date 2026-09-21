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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ripple
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velo.app.model.RideStatus
import com.velo.app.ui.theme.BtnFinishPink
import com.velo.app.ui.theme.BtnFinishTextDark
import com.velo.app.ui.theme.BtnNeutralGrey
import com.velo.app.ui.theme.BtnNeutralText
import com.velo.app.ui.theme.getRobotoFlexFont

@Composable
fun BottomActionControls(
    rideStatus: RideStatus,
    onPauseClick: () -> Unit,
    onResumeClick: () -> Unit,
    onFinishClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
    ) {
        if (rideStatus == RideStatus.RECORDING) {
            // STATE A: Active Ride (Full-Width Tonal Grey Pause Button, STRICTLY ZERO BORDER)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .clip(CircleShape)
                    .background(BtnNeutralGrey)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(color = BtnNeutralText),
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
                        tint = BtnNeutralText,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Pause",
                        color = BtnNeutralText,
                        fontSize = 17.sp,
                        fontFamily = getRobotoFlexFont(width = 100f, weight = 600)
                    )
                }
            }
        } else {
            // STATE B: Paused Ride (Full Pink Finish + Same Grey Resume, ZERO BORDERS)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Finish Button (Full Pink with 20dp Square Icon, STRICTLY ZERO BORDER)
                Box(
                    modifier = Modifier
                        .height(58.dp)
                        .clip(CircleShape)
                        .background(BtnFinishPink)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(color = BtnFinishTextDark),
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
                            tint = BtnFinishTextDark,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Finish",
                            color = BtnFinishTextDark,
                            fontSize = 16.sp,
                            fontFamily = getRobotoFlexFont(width = 100f, weight = 800)
                        )
                    }
                }

                // Resume Button (EXACT SAME GREY AS PAUSE, ZERO BORDER)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(58.dp)
                        .clip(CircleShape)
                        .background(BtnNeutralGrey)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(color = BtnNeutralText),
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
                            tint = BtnNeutralText,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Resume",
                            color = BtnNeutralText,
                            fontSize = 17.sp,
                            fontFamily = getRobotoFlexFont(width = 100f, weight = 600)
                        )
                    }
                }
            }
        }
    }
}
