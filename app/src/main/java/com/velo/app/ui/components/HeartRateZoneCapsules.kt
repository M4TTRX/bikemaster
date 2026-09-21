package com.velo.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velo.app.model.HeartRateZone
import com.velo.app.ui.theme.HrInactiveCapsule
import com.velo.app.ui.theme.HrZone1Warmup
import com.velo.app.ui.theme.HrZone2Aerobic
import com.velo.app.ui.theme.HrZone3Cardio
import com.velo.app.ui.theme.HrZone4Peak
import com.velo.app.ui.theme.getRobotoFlexFont

private val HrZoneList = listOf(
    HeartRateZone.WARMUP to HrZone1Warmup,
    HeartRateZone.AEROBIC to HrZone2Aerobic,
    HeartRateZone.CARDIO to HrZone3Cardio,
    HeartRateZone.PEAK to HrZone4Peak
)

@Composable
fun HeartRateZoneCapsules(
    heartRateBpm: Int,
    zone: HeartRateZone?,
    modifier: Modifier = Modifier
) {
    val activeColor = when (zone) {
        HeartRateZone.WARMUP -> HrZone1Warmup
        HeartRateZone.AEROBIC -> HrZone2Aerobic
        HeartRateZone.CARDIO -> HrZone3Cardio
        HeartRateZone.PEAK -> HrZone4Peak
        null -> HrInactiveCapsule
    }

    val animatedColor by animateColorAsState(
        targetValue = activeColor,
        animationSpec = tween(durationMillis = 250),
        label = "HrColorAnimation"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 4 Horizontal Thick Capsules (18dp height)
        Row(
            modifier = Modifier
                .width(280.dp)
                .height(18.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HrZoneList.forEach { (z, targetColor) ->
                val isCurrentZone = zone == z
                val capsuleColor = if (isCurrentZone) targetColor else HrInactiveCapsule

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(18.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(capsuleColor)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Much Larger & Thicker BPM Readout (50sp)
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Center
        ) {
            val bpmText = if (heartRateBpm > 0) heartRateBpm.toString() else "--"
            Text(
                text = bpmText,
                color = animatedColor,
                fontSize = 50.sp,
                fontFamily = getRobotoFlexFont(width = 75f, weight = 700),
                lineHeight = 50.sp
            )

            Spacer(modifier = Modifier.width(4.dp))

            Text(
                text = "bpm",
                color = animatedColor,
                fontSize = 16.sp,
                fontFamily = getRobotoFlexFont(width = 95f, weight = 500),
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }
    }
}
