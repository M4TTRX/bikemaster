package com.velo.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velo.app.model.UnitSystem
import com.velo.app.ui.theme.BtnFinishPink
import com.velo.app.ui.theme.BtnNeutralGrey
import com.velo.app.ui.theme.BtnNeutralText
import com.velo.app.ui.theme.OledBlack
import com.velo.app.ui.theme.PillIconEmerald
import com.velo.app.ui.theme.SettingsCardBackground
import com.velo.app.ui.theme.SettingsDividerGrey
import com.velo.app.ui.theme.getRobotoFlexFont

@Composable
fun SettingsScreen(
    unitSystem: UnitSystem,
    onUnitSystemChange: (UnitSystem) -> Unit,
    isMockSimulatorEnabled: Boolean,
    onMockSimulatorChange: (Boolean) -> Unit,
    isKeepScreenOn: Boolean,
    onKeepScreenOnChange: (Boolean) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(OledBlack)
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
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Settings",
                color = Color.White,
                fontSize = 26.sp,
                fontFamily = getRobotoFlexFont(width = 90f, weight = 700)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // SECTION 1: UNITS OF TRAVEL
        SettingsSectionHeader(title = "Units of Travel")
        SettingsCard {
            SettingsUnitRow(
                title = "Metric (km, km/h, meters)",
                isSelected = unitSystem == UnitSystem.METRIC,
                onClick = { onUnitSystemChange(UnitSystem.METRIC) }
            )
            HorizontalDivider(color = SettingsDividerGrey, thickness = 1.dp)
            SettingsUnitRow(
                title = "Imperial (mi, mph, feet)",
                isSelected = unitSystem == UnitSystem.IMPERIAL,
                onClick = { onUnitSystemChange(UnitSystem.IMPERIAL) }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // SECTION 2: CONNECTED SENSORS & PAIRING
        SettingsSectionHeader(title = "Sensors & Connections")
        SettingsCard {
            SettingsSensorItem(
                icon = Icons.Default.Watch,
                title = "Heart Rate Monitor",
                subtitle = "Wear OS Smartwatch (Active)",
                isConnected = true
            )
            HorizontalDivider(color = SettingsDividerGrey, thickness = 1.dp)
            SettingsSensorItem(
                icon = Icons.AutoMirrored.Filled.DirectionsBike,
                title = "Cadence & Speed (CSC)",
                subtitle = "BLE Sensor (Ready to pair)",
                isConnected = false
            )
            HorizontalDivider(color = SettingsDividerGrey, thickness = 1.dp)
            SettingsSensorItem(
                icon = Icons.Default.Speed,
                title = "Cycling Power Meter",
                subtitle = "BLE Power 0x1818 (Ready to pair)",
                isConnected = false
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // SECTION 3: DISPLAY & BATTERY OPTIMIZATION
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

        Spacer(modifier = Modifier.height(24.dp))

        // SECTION 4: TESTING & SIMULATOR
        SettingsSectionHeader(title = "Developer & Emulator")
        SettingsCard {
            SettingsSwitchRow(
                icon = Icons.Default.Straighten,
                title = "Live Sensor Simulator",
                subtitle = "Simulates speed, HR, cadence, and power in emulator",
                isChecked = isMockSimulatorEnabled,
                onCheckedChange = onMockSimulatorChange
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        color = PillIconEmerald,
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
            .background(SettingsCardBackground)
    ) {
        content()
    }
}

@Composable
private fun SettingsUnitRow(
    title: String,
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
        Text(
            text = title,
            color = Color.White,
            fontSize = 15.sp,
            fontFamily = getRobotoFlexFont(width = 90f, weight = 500)
        )

        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(PillIconEmerald),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = OledBlack,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun SettingsSensorItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isConnected: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* Pair dialog / scanning */ }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(BtnNeutralGrey),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isConnected) PillIconEmerald else BtnNeutralText,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 15.sp,
                fontFamily = getRobotoFlexFont(width = 90f, weight = 600)
            )
            Text(
                text = subtitle,
                color = if (isConnected) PillIconEmerald else Color.Gray,
                fontSize = 12.sp,
                fontFamily = getRobotoFlexFont(width = 90f, weight = 400)
            )
        }

        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(if (isConnected) BtnNeutralGrey else BtnFinishPink)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = if (isConnected) "Connected" else "Pair",
                color = if (isConnected) Color.White else Color(0xFF4A001A),
                fontSize = 12.sp,
                fontFamily = getRobotoFlexFont(width = 95f, weight = 700)
            )
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
                .background(BtnNeutralGrey),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = BtnNeutralText,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 15.sp,
                fontFamily = getRobotoFlexFont(width = 90f, weight = 600)
            )
            Text(
                text = subtitle,
                color = Color.Gray,
                fontSize = 12.sp,
                fontFamily = getRobotoFlexFont(width = 90f, weight = 400)
            )
        }

        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = OledBlack,
                checkedTrackColor = PillIconEmerald,
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = BtnNeutralGrey
            )
        )
    }
}
