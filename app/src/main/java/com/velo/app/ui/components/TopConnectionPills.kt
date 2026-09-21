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
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velo.app.ui.theme.PillBackground
import com.velo.app.ui.theme.PillIconEmerald
import com.velo.app.ui.theme.PillTextWhite

@Composable
fun TopConnectionPills(
    isGpsActive: Boolean,
    isWearOsConnected: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // GPS Status Pill - STRICTLY ZERO BORDER
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(PillBackground)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "GPS Status",
                tint = PillIconEmerald,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isGpsActive) "GPS High Accuracy" else "GPS Searching",
                color = PillTextWhite,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Wear OS Status Pill - STRICTLY ZERO BORDER
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(PillBackground)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Watch,
                contentDescription = "Wear OS Status",
                tint = PillIconEmerald,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isWearOsConnected) "Wear OS synced" else "Wear OS paired",
                color = PillTextWhite,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
