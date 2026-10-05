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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Watch
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velo.app.data.PairedBleDevice
import com.velo.app.model.SensorDataType
import com.velo.app.model.SpeedSource
import com.velo.app.model.ThemeMode
import com.velo.app.model.UnitSystem
import com.velo.app.sensors.DiscoveredSensor
import com.velo.app.ui.components.SingleSelectButtonGroup
import com.velo.app.ui.theme.getRobotoFlexFont
import com.velo.app.ui.util.performAppClickHaptic

data class WheelSizePreset(
    val name: String,
    val dimensions: String,
    val circumferenceMm: Int
)

val WHEEL_SIZE_PRESETS = listOf(
    WheelSizePreset("Standard Road", "700 x 25c", 2105),
    WheelSizePreset("Endurance Road", "700 x 28c", 2136),
    WheelSizePreset("Gravel / All-Road", "700 x 32c", 2155),
    WheelSizePreset("Race", "700 x 23c", 2096),
    WheelSizePreset("MTB", "29\" x 2.2\"", 2298)
)

@Composable
fun SettingsScreen(
    themeMode: ThemeMode = ThemeMode.DARK,
    onThemeModeChange: (ThemeMode) -> Unit = {},
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
    isWearOsCompanionInstalled: Boolean = false,
    isWearOsConnected: Boolean = false,
    onInstallWearOsCompanion: () -> Unit = {},
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
    val view = LocalView.current
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
                onClick = {
                    view.performAppClickHaptic()
                    onBackClick()
                },
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to Ride Dashboard",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Settings",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 28.sp,
                fontFamily = getRobotoFlexFont(width = 90f, weight = 700)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // SECTION 1: MEASUREMENTS (Combines distance, units, speed source, and wheel size)
        SettingsSectionHeader(title = "Measurements")
        SettingsCard {
            // 1A. Units for measurements (2 options -> Button group)
            val unitsSubtitle = when (unitSystem) {
                UnitSystem.METRIC -> "Metric (km, km/h, meters)"
                UnitSystem.IMPERIAL -> "Imperial (mi, mph, feet)"
            }
            SettingsButtonGroupRow(
                title = "Units of Measurement",
                subtitle = unitsSubtitle
            ) {
                SingleSelectButtonGroup(
                    options = listOf(UnitSystem.METRIC, UnitSystem.IMPERIAL),
                    selectedOption = unitSystem,
                    onOptionSelected = onUnitSystemChange,
                    labelProvider = { if (it == UnitSystem.METRIC) "Metric" else "Imperial" }
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)

            // 1B. Speed Tracking Source (3 options -> Button group)
            val speedSubtitle = when (speedSource) {
                SpeedSource.AUTO -> "Speed sensor if connected, GPS fallback"
                SpeedSource.SPEED_SENSOR -> "Wheel sensor revolutions only"
                SpeedSource.GPS -> "Phone GPS only"
            }
            SettingsButtonGroupRow(
                title = "Speed Tracking Source",
                subtitle = speedSubtitle
            ) {
                SingleSelectButtonGroup(
                    options = listOf(SpeedSource.AUTO, SpeedSource.SPEED_SENSOR, SpeedSource.GPS),
                    selectedOption = speedSource,
                    onOptionSelected = onSpeedSourceChange,
                    labelProvider = {
                        when (it) {
                            SpeedSource.AUTO -> "Auto"
                            SpeedSource.SPEED_SENSOR -> "Sensor"
                            SpeedSource.GPS -> "GPS"
                        }
                    }
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)

            // 1C. Wheel Size Selection (Do not specify mm circumference)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
            ) {
                Text(
                    text = "Wheel Size",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp,
                    fontFamily = getRobotoFlexFont(width = 90f, weight = 600)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Select your bike's wheel and tire size",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    fontFamily = getRobotoFlexFont(width = 90f, weight = 400)
                )
            }

            WHEEL_SIZE_PRESETS.forEachIndexed { index, preset ->
                SettingsSelectRow(
                    title = preset.name,
                    subtitle = preset.dimensions,
                    isSelected = wheelCircumferenceMm == preset.circumferenceMm,
                    onClick = { onWheelCircumferenceChange(preset.circumferenceMm) }
                )
                if (index < WHEEL_SIZE_PRESETS.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // SECTION 2: DEVICES
        SettingsSectionHeader(title = "Devices")
        SettingsCard {
            // 2A. Wear OS Companion
            if (isWearOsCompanionInstalled) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Watch,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Wear OS Companion",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 18.sp,
                            fontFamily = getRobotoFlexFont(width = 90f, weight = 600)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isWearOsConnected) "Connected • Optical HR Ready" else "Installed • Optical HR Ready",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 14.sp,
                            fontFamily = getRobotoFlexFont(width = 90f, weight = 400)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Installed",
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontSize = 12.sp,
                            fontFamily = getRobotoFlexFont(width = 95f, weight = 700)
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Watch,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Wear OS Companion",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 18.sp,
                                fontFamily = getRobotoFlexFont(width = 90f, weight = 600)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Stream optical heart rate from your smartwatch",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp,
                                fontFamily = getRobotoFlexFont(width = 90f, weight = 400)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .clickable {
                                view.performAppClickHaptic()
                                onInstallWearOsCompanion()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Watch,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Install Wear OS Companion",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 14.sp,
                                fontFamily = getRobotoFlexFont(width = 95f, weight = 700)
                            )
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
            }

            // 2B. Paired BLE Devices List
            if (pairedBleDevices.isNotEmpty()) {
                pairedBleDevices.forEachIndexed { index, device ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp)
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
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = device.name,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 18.sp,
                                    fontFamily = getRobotoFlexFont(width = 90f, weight = 600)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = device.address,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 14.sp,
                                    fontFamily = getRobotoFlexFont(width = 90f, weight = 400)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.errorContainer)
                                    .clickable {
                                        view.performAppClickHaptic()
                                        onUnpairSensor(device.address)
                                    }
                                    .padding(horizontal = 14.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = "Forget",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontSize = 12.sp,
                                    fontFamily = getRobotoFlexFont(width = 95f, weight = 700)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Provided Data Types:",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            fontFamily = getRobotoFlexFont(width = 90f, weight = 500)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

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
                                            view.performAppClickHaptic()
                                            val updated = if (isEnabled) {
                                                device.enabledDataTypes - dataType
                                            } else {
                                                device.enabledDataTypes + dataType
                                            }
                                            onUpdateSensorTypes(device.address, updated)
                                        }
                                        .padding(horizontal = 12.dp, vertical = 7.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = dataType.title,
                                            color = chipText,
                                            fontSize = 13.sp,
                                            fontFamily = getRobotoFlexFont(width = 90f, weight = if (isEnabled) 700 else 400)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
                }
            }

            // 2C. Bottom full-width Add Sensor button (Same primary color as selected button group item)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable {
                        view.performAppClickHaptic()
                        onStartScan()
                        isPairingDialogOpen = true
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Add Sensor",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 15.sp,
                        fontFamily = getRobotoFlexFont(width = 95f, weight = 700)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // SECTION 3: DISPLAY & BATTERY (Theme mode selection + Keep Screen On)
        SettingsSectionHeader(title = "Display & Battery")
        SettingsCard {
            // Theme selection (3 options -> Button group)
            val themeSubtitle = when (themeMode) {
                ThemeMode.DARK -> "App uses dark theme"
                ThemeMode.LIGHT -> "App uses light theme"
                ThemeMode.SYSTEM -> "Follows device system theme"
            }
            SettingsButtonGroupRow(
                title = "Theme",
                subtitle = themeSubtitle
            ) {
                SingleSelectButtonGroup(
                    options = listOf(ThemeMode.DARK, ThemeMode.LIGHT, ThemeMode.SYSTEM),
                    selectedOption = themeMode,
                    onOptionSelected = onThemeModeChange,
                    labelProvider = { it.title }
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)

            SettingsSwitchRow(
                icon = Icons.Default.Tune,
                title = "Keep Screen On",
                subtitle = "Maintains cockpit display while recording",
                isChecked = isKeepScreenOn,
                onCheckedChange = onKeepScreenOnChange
            )
        }

        // SECTION 4: TESTING & SIMULATOR (Only visible on Android Emulator)
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
        fontSize = 14.sp,
        fontFamily = getRobotoFlexFont(width = 100f, weight = 700),
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
private fun SettingsButtonGroupRow(
    title: String,
    subtitle: String? = null,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 18.sp,
            fontFamily = getRobotoFlexFont(width = 90f, weight = 600)
        )
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                fontFamily = getRobotoFlexFont(width = 90f, weight = 400)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
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
    val view = LocalView.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = {
                view.performAppClickHaptic()
                onClick()
            })
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 18.sp,
                fontFamily = getRobotoFlexFont(width = 90f, weight = 500)
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    fontFamily = getRobotoFlexFont(width = 90f, weight = 400)
                )
            }
        }

        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(18.dp)
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
    val view = LocalView.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 18.sp,
                fontFamily = getRobotoFlexFont(width = 90f, weight = 600)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                fontFamily = getRobotoFlexFont(width = 90f, weight = 400)
            )
        }

        Switch(
            checked = isChecked,
            onCheckedChange = {
                view.performAppClickHaptic()
                onCheckedChange(it)
            }
        )
    }
}
