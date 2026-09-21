package com.velo.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.velo.app.sensors.DiscoveredSensor
import com.velo.app.sensors.SensorType
import com.velo.app.ui.theme.BtnFinishPink
import com.velo.app.ui.theme.BtnNeutralGrey
import com.velo.app.ui.theme.BtnNeutralText
import com.velo.app.ui.theme.PillIconEmerald
import com.velo.app.ui.theme.SettingsCardBackground
import com.velo.app.ui.theme.SettingsDividerGrey
import com.velo.app.ui.theme.getRobotoFlexFont

@Composable
fun SensorPairingDialog(
    sensorType: SensorType,
    discoveredSensors: List<DiscoveredSensor>,
    isScanning: Boolean,
    onSensorSelected: (DiscoveredSensor) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(SettingsCardBackground)
                .padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Pair ${sensorType.title}",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontFamily = getRobotoFlexFont(width = 90f, weight = 700)
                    )
                    Text(
                        text = if (isScanning) "Searching for nearby devices..." else "Discovered devices",
                        color = if (isScanning) PillIconEmerald else Color.Gray,
                        fontSize = 12.sp,
                        fontFamily = getRobotoFlexFont(width = 90f, weight = 400)
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isScanning && discoveredSensors.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = PillIconEmerald,
                        modifier = Modifier.size(36.dp),
                        strokeWidth = 3.dp
                    )
                }
            } else if (discoveredSensors.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No sensors found nearby",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                    discoveredSensors.forEachIndexed { index, sensor ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onSensorSelected(sensor) }
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(BtnNeutralGrey),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bluetooth,
                                    contentDescription = null,
                                    tint = PillIconEmerald,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = sensor.name,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontFamily = getRobotoFlexFont(width = 90f, weight = 600)
                                )
                                Text(
                                    text = "${sensor.address} • ${sensor.rssi} dBm",
                                    color = Color.Gray,
                                    fontSize = 11.sp,
                                    fontFamily = getRobotoFlexFont(width = 90f, weight = 400)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(BtnFinishPink)
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Connect",
                                    color = Color(0xFF4A001A),
                                    fontSize = 12.sp,
                                    fontFamily = getRobotoFlexFont(width = 95f, weight = 700)
                                )
                            }
                        }

                        if (index < discoveredSensors.lastIndex) {
                            HorizontalDivider(color = SettingsDividerGrey, thickness = 1.dp)
                        }
                    }
                }
            }
        }
    }
}
