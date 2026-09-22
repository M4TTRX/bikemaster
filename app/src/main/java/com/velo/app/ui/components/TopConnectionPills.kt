package com.velo.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TopConnectionPills(
    isGpsActive: Boolean,
    isHeartRateConnected: Boolean,
    modifier: Modifier = Modifier,
    isBleCadenceConnected: Boolean = false,
    isBleSpeedConnected: Boolean = false,
    isBlePowerConnected: Boolean = false
) {
    val pillBackground = MaterialTheme.colorScheme.surfaceContainer
    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.outline
    val textColor = MaterialTheme.colorScheme.onSurface

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // GPS Status Pill
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(pillBackground)
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "GPS Status",
                tint = if (isGpsActive) activeColor else inactiveColor,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (isGpsActive) "GPS Live" else "GPS Waiting",
                color = textColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Heart Rate Monitor Status Pill
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(pillBackground)
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = "Heart Rate Monitor Status",
                tint = if (isHeartRateConnected) activeColor else inactiveColor,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (isHeartRateConnected) "HR Live" else "No HR",
                color = textColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }

        if (isBleCadenceConnected || isBleSpeedConnected) {
            Spacer(modifier = Modifier.width(6.dp))
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(pillBackground)
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.DirectionsBike,
                    contentDescription = "CSC Sensor Status",
                    tint = activeColor,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                val label = when {
                    isBleSpeedConnected && isBleCadenceConnected -> "Speed+Cad"
                    isBleSpeedConnected -> "Speed Sensor"
                    else -> "Cadence"
                }
                Text(
                    text = label,
                    color = textColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        if (isBlePowerConnected) {
            Spacer(modifier = Modifier.width(6.dp))
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(pillBackground)
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = "Power Meter Status",
                    tint = activeColor,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Power",
                    color = textColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
