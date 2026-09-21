package com.velo.app.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.velo.app.model.RideStatus
import com.velo.app.model.RideTelemetry
import com.velo.app.model.UnitSystem
import com.velo.app.ui.dashboard.RideDashboardScreen
import com.velo.app.ui.settings.SettingsScreen
import com.velo.app.ui.theme.OledBlack
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainScreen(
    telemetry: RideTelemetry,
    unitSystem: UnitSystem,
    rideStatus: RideStatus,
    isMockSimulatorEnabled: Boolean,
    isKeepScreenOn: Boolean,
    onUnitSystemChange: (UnitSystem) -> Unit,
    onMockSimulatorChange: (Boolean) -> Unit,
    onKeepScreenOnChange: (Boolean) -> Unit,
    discoveredSensors: List<com.velo.app.sensors.DiscoveredSensor>,
    isScanning: Boolean,
    onStartScan: (com.velo.app.sensors.SensorType) -> Unit,
    onPairSensor: (com.velo.app.sensors.DiscoveredSensor) -> Unit,
    onPauseClick: () -> Unit,
    onResumeClick: () -> Unit,
    onFinishClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()

    HorizontalPager(
        state = pagerState,
        modifier = modifier
            .fillMaxSize()
            .background(OledBlack)
    ) { page ->
        when (page) {
            0 -> RideDashboardScreen(
                telemetry = telemetry,
                unitSystem = unitSystem,
                rideStatus = rideStatus,
                onPauseClick = onPauseClick,
                onResumeClick = onResumeClick,
                onFinishClick = onFinishClick
            )
            1 -> SettingsScreen(
                unitSystem = unitSystem,
                onUnitSystemChange = onUnitSystemChange,
                isMockSimulatorEnabled = isMockSimulatorEnabled,
                onMockSimulatorChange = onMockSimulatorChange,
                isKeepScreenOn = isKeepScreenOn,
                onKeepScreenOnChange = onKeepScreenOnChange,
                discoveredSensors = discoveredSensors,
                isScanning = isScanning,
                onStartScan = onStartScan,
                onPairSensor = onPairSensor,
                isCadenceConnected = telemetry.isBleCadenceConnected,
                isPowerConnected = telemetry.isBlePowerConnected,
                isHeartRateConnected = telemetry.isWearOsConnected,
                onBackClick = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(0)
                    }
                }
            )
        }
    }
}
