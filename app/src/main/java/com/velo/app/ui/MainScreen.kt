package com.velo.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.velo.app.data.PairedBleDevice
import com.velo.app.data.SettingsRepository
import com.velo.app.model.MetricType
import com.velo.app.model.RideStatus
import com.velo.app.model.RideTelemetry
import com.velo.app.model.SensorDataType
import com.velo.app.model.SpeedSource
import com.velo.app.model.UnitSystem
import com.velo.app.sensors.DiscoveredSensor
import com.velo.app.ui.dashboard.RideDashboardScreen
import com.velo.app.ui.settings.SettingsScreen
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    telemetry: RideTelemetry,
    unitSystem: UnitSystem,
    speedSource: SpeedSource,
    wheelCircumferenceMm: Int,
    pairedBleDevices: List<PairedBleDevice>,
    cockpitTiles: List<MetricType> = SettingsRepository.DEFAULT_TILES,
    cockpitTileSources: Map<Int, String> = emptyMap(),
    rideStatus: RideStatus,
    isMockSimulatorEnabled: Boolean,
    isKeepScreenOn: Boolean,
    isEmulator: Boolean,
    onUnitSystemChange: (UnitSystem) -> Unit,
    onSpeedSourceChange: (SpeedSource) -> Unit,
    onWheelCircumferenceChange: (Int) -> Unit,
    onUnpairSensor: (String) -> Unit,
    onUpdateSensorTypes: (address: String, types: Set<SensorDataType>) -> Unit,
    onTileConfigured: (slotIndex: Int, metricType: MetricType, sourceId: String?) -> Unit,
    onMockSimulatorChange: (Boolean) -> Unit,
    onKeepScreenOnChange: (Boolean) -> Unit,
    discoveredSensors: List<DiscoveredSensor>,
    isScanning: Boolean,
    onStartScan: () -> Unit,
    onPairSensor: (DiscoveredSensor) -> Unit,
    onStartClick: () -> Unit,
    onPauseClick: () -> Unit,
    onResumeClick: () -> Unit,
    onFinishClick: () -> Unit,
    onResetClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()

    HorizontalPager(
        state = pagerState,
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) { page ->
        when (page) {
            0 -> RideDashboardScreen(
                telemetry = telemetry,
                unitSystem = unitSystem,
                rideStatus = rideStatus,
                cockpitTiles = cockpitTiles,
                cockpitTileSources = cockpitTileSources,
                pairedDevices = pairedBleDevices,
                onTileConfigured = onTileConfigured,
                onStartClick = onStartClick,
                onPauseClick = onPauseClick,
                onResumeClick = onResumeClick,
                onFinishClick = onFinishClick,
                onResetClick = onResetClick
            )
            1 -> SettingsScreen(
                unitSystem = unitSystem,
                onUnitSystemChange = onUnitSystemChange,
                speedSource = speedSource,
                onSpeedSourceChange = onSpeedSourceChange,
                wheelCircumferenceMm = wheelCircumferenceMm,
                onWheelCircumferenceChange = onWheelCircumferenceChange,
                pairedBleDevices = pairedBleDevices,
                onUnpairSensor = onUnpairSensor,
                onUpdateSensorTypes = onUpdateSensorTypes,
                isMockSimulatorEnabled = isMockSimulatorEnabled,
                onMockSimulatorChange = onMockSimulatorChange,
                isKeepScreenOn = isKeepScreenOn,
                onKeepScreenOnChange = onKeepScreenOnChange,
                discoveredSensors = discoveredSensors,
                isScanning = isScanning,
                onStartScan = onStartScan,
                onPairSensor = onPairSensor,
                isCadenceConnected = telemetry.isBleCadenceConnected,
                isSpeedConnected = telemetry.isBleSpeedConnected,
                isHeartRateConnected = telemetry.isHeartRateConnected,
                isPowerConnected = telemetry.isBlePowerConnected,
                isEmulator = isEmulator,
                onBackClick = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(0)
                    }
                }
            )
        }
    }
}
