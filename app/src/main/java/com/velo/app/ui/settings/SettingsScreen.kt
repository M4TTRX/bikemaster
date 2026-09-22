package com.velo.app.ui.settings

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velo.app.data.PairedBleDevice
import com.velo.app.model.SensorDataType
import com.velo.app.model.SpeedSource
import com.velo.app.model.UnitSystem
import com.velo.app.sensors.DiscoveredSensor
import com.velo.app.ui.theme.getRobotoFlexFont

val WHEEL_SIZE_PRESETS = listOf(
    Pair("700 x 25c (Standard Road)", 2105),
    Pair("700 x 28c (Endurance Road)", 2136),
    Pair("700 x 32c (Gravel / All-Road)", 2155),
    Pair("700 x 23c (Race)", 2096),
    Pair("29\" x 2.2\" (MTB)", 2298)
)

@Composable
fun SettingsScreen(
    unitSystem: UnitSystem,
    onUnitSystemChange: (UnitSystem) -> Unit,
    speedSource: SpeedSource,
    onSpeedSourceChange: (SpeedSource) -> Unit,
    wheelCircumferenceMm: Int,
    onWheelCircumferenceChange: (Int) -> Unit,
    pairedBleDevices: List<PairedBleDevice>,
    onUnpairSensor: (String) -> Unit,
    onUpdateSensorTypes: (address: String, types: Set<SensorDataType>) -> Unit,
    isMockSimulatorEnabled: Boolean,
    onMockSimulatorChange: (Boolean) -> Unit,
    isKeepScreenOn: Boolean,
    onKeepScreenOnChange: (Boolean) -> Unit,
    discoveredSensors: List<DiscoveredSensor>,
    isScanning: Boolean,
    onStartScan: () -> Unit,
    onPairSensor: (DiscoveredSensor) -> Unit,
    isCadenceConnected: Boolean,
    isSpeedConnected: Boolean,
    isHeartRateConnected: Boolean,
    isPowerConnected: Boolean,
    isEmulator: Boolean,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var isPairingDialogOpen by remember { mutableStateOf(false) }

    if (isPairingDialogOpen) {
        SensorPairingDialog(
            discoveredSensors = discoveredSensors,
            isScanning = isScanning,
            onSensorSelected = { sensor ->
                onPairSensor(sensor)
                isPairingDialogOpen = false
            },
            onDismiss = { isPairingDialogOpen = false }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
            .navigationBarsPadding()
            .verticalScroll(scrollState)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to Ride Dashboard",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Settings",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 26.sp,
                fontFamily = getRobotoFlexFont(width = 90f, weight = 700)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // SECTION 1: SPEED & TRACKING SOURCE
        SettingsSectionHeader(title = "Speed Tracking Source")
        SettingsCard {
            SettingsSelectRow(
                title = "Auto (Speed Sensor Priority)",
                subtitle = "Uses wheel speed sensor if connected, falls back to GPS",
                isSelected = speedSource == SpeedSource.AUTO,
                onClick = { onSpeedSourceChange(SpeedSource.AUTO) }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
            SettingsSelectRow(
                title = "Speed Sensor Only",
                subtitle = "Tracks speed & distance strictly from wheel sensor revolutions",
                isSelected = speedSource == SpeedSource.SPEED_SENSOR,
                onClick = { onSpeedSourceChange(SpeedSource.SPEED_SENSOR) }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
            SettingsSelectRow(
                title = "GPS Only",
                subtitle = "Tracks speed & distance strictly from phone GPS",
                isSelected = speedSource == SpeedSource.GPS,
                onClick = { onSpeedSourceChange(SpeedSource.GPS) }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // SECTION 2: WHEEL CALIBRATION (If using speed sensor)
        SettingsSectionHeader(title = "Wheel Calibration (${wheelCircumferenceMm} mm)")
        SettingsCard {
            WHEEL_SIZE_PRESETS.forEachIndexed { index, preset ->
                SettingsSelectRow(
                    title = preset.first,
                    subtitle = "${preset.second} mm circumference",
                    isSelected = wheelCircumferenceMm == preset.second,
                    onClick = { onWheelCircumferenceChange(preset.second) }
                )
                if (index < WHEEL_SIZE_PRESETS.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // SECTION 3: CONNECTED SENSORS & PAIRING
        SettingsSectionHeader(title = "Sensors & Connections")
        SettingsCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onStartScan()
                        isPairingDialogOpen = true
                    }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Sensors,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Pair a Sensor",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp,
                        fontFamily = getRobotoFlexFont(width = 90f, weight = 600)
                    )
                    Text(
                        text = "Connect Speed, Cadence, Power, or HR monitor",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontFamily = getRobotoFlexFont(width = 90f, weight = 400)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable {
                            onStartScan()
                            isPairingDialogOpen = true
                        }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = "Scan",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 12.sp,
                        fontFamily = getRobotoFlexFont(width = 95f, weight = 700)
                    )
                }
            }
        }

        // SECTION 3B: PAIRED SENSORS LIST WITH DATA TYPE EDITING & FORGET
        if (pairedBleDevices.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            SettingsSectionHeader(title = "Paired Devices (${pairedBleDevices.size})")
            SettingsCard {
                pairedBleDevices.forEachIndexed { index, device ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val icon = when {
                                device.detectedCapabilities.contains(SensorDataType.POWER) -> Icons.Default.Bolt
                                device.detectedCapabilities.contains(SensorDataType.HEART_RATE) -> Icons.Default.Favorite
                                device.detectedCapabilities.contains(SensorDataType.SPEED) ||
                                    device.detectedCapabilities.contains(SensorDataType.CADENCE) -> Icons.AutoMirrored.Filled.DirectionsBike
                                else -> Icons.Default.Sensors
                            }

                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = device.name,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp,
                                    fontFamily = getRobotoFlexFont(width = 90f, weight = 600)
                                )
                                Text(
                                    text = device.address,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    fontFamily = getRobotoFlexFont(width = 90f, weight = 400)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.errorContainer)
                                    .clickable { onUnpairSensor(device.address) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Forget",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontSize = 11.sp,
                                    fontFamily = getRobotoFlexFont(width = 95f, weight = 700)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Data type chips to configure what data this sensor provides
                        Text(
                            text = "Provided Data Types:",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            fontFamily = getRobotoFlexFont(width = 90f, weight = 500)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        val availableTypes = remember(device.detectedCapabilities) {
                            if (device.detectedCapabilities.isNotEmpty()) {
                                device.detectedCapabilities.toList()
                            } else {
                                SensorDataType.entries
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            availableTypes.forEach { dataType ->
                                val isEnabled = device.enabledDataTypes.contains(dataType)
                                val interactionSource = remember { MutableInteractionSource() }
                                val isPressed by interactionSource.collectIsPressedAsState()
                                val chipScale by animateFloatAsState(
                                    targetValue = if (isPressed) 0.93f else 1f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    ),
                                    label = "SensorTypeChipScale"
                                )

                                val chipBg = if (isEnabled) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                }
                                val chipText = if (isEnabled) {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }

                                Box(
                                    modifier = Modifier
                                        .scale(chipScale)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(chipBg)
                                        .clickable(
                                            interactionSource = interactionSource,
                                            indication = null
                                        ) {
                                            val updated = if (isEnabled) {
                                                device.enabledDataTypes - dataType
                                            } else {
                                                device.enabledDataTypes + dataType
                                            }
                                            onUpdateSensorTypes(device.address, updated)
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (isEnabled) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = chipText,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                        }
                                        Text(
                                            text = dataType.title,
                                            color = chipText,
                                            fontSize = 11.sp,
                                            fontFamily = getRobotoFlexFont(width = 90f, weight = if (isEnabled) 700 else 400)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (index < pairedBleDevices.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // SECTION 4: UNITS OF TRAVEL
        SettingsSectionHeader(title = "Units of Travel")
        SettingsCard {
            SettingsSelectRow(
                title = "Metric (km, km/h, meters)",
                subtitle = "Kilometers, km/h, and meters for elevation",
                isSelected = unitSystem == UnitSystem.METRIC,
                onClick = { onUnitSystemChange(UnitSystem.METRIC) }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
            SettingsSelectRow(
                title = "Imperial (mi, mph, feet)",
                subtitle = "Miles, mph, and feet for elevation",
                isSelected = unitSystem == UnitSystem.IMPERIAL,
                onClick = { onUnitSystemChange(UnitSystem.IMPERIAL) }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // SECTION 5: DISPLAY & BATTERY OPTIMIZATION
        SettingsSectionHeader(title = "Display & Battery")
        SettingsCard {
            SettingsSwitchRow(
                icon = Icons.Default.Tune,
                title = "Keep Screen On",
                subtitle = "Maintains cockpit display while recording",
                isChecked = isKeepScreenOn,
                onCheckedChange = onKeepScreenOnChange
            )
        }

        // SECTION 6: TESTING & SIMULATOR (Only visible on Android Emulator)
        if (isEmulator) {
            Spacer(modifier = Modifier.height(24.dp))
            SettingsSectionHeader(title = "Developer & Emulator")
            SettingsCard {
                SettingsSwitchRow(
                    icon = Icons.Default.Straighten,
                    title = "Live Sensor Simulator",
                    subtitle = "Simulates speed, HR, and cadence in emulator (Off by default)",
                    isChecked = isMockSimulatorEnabled,
                    onCheckedChange = onMockSimulatorChange
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        color = MaterialTheme.colorScheme.primary,
        fontSize = 13.sp,
        fontFamily = getRobotoFlexFont(width = 100f, weight = 600),
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsCard(
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
    ) {
        content()
    }
}

@Composable
private fun SettingsSelectRow(
    title: String,
    subtitle: String? = null,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                fontFamily = getRobotoFlexFont(width = 90f, weight = 500)
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontFamily = getRobotoFlexFont(width = 90f, weight = 400)
                )
            }
        }

        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                fontFamily = getRobotoFlexFont(width = 90f, weight = 600)
            )
            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                fontFamily = getRobotoFlexFont(width = 90f, weight = 400)
            )
        }

        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange
        )
    }
}
