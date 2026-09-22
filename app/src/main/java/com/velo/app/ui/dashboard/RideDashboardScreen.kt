package com.velo.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.velo.app.data.PairedBleDevice
import com.velo.app.data.SettingsRepository
import com.velo.app.model.MetricType
import com.velo.app.model.RideStatus
import com.velo.app.model.RideTelemetry
import com.velo.app.model.UnitSystem
import com.velo.app.ui.components.BottomActionControls
import com.velo.app.ui.components.BottomMetricsGrid
import com.velo.app.ui.components.HeartRateZoneCapsules
import com.velo.app.ui.components.SpeedDisplay
import com.velo.app.ui.components.TileCustomizationSheet
import com.velo.app.ui.components.TopConnectionPills

@Composable
fun RideDashboardScreen(
    telemetry: RideTelemetry,
    unitSystem: UnitSystem,
    rideStatus: RideStatus,
    onStartClick: () -> Unit,
    onPauseClick: () -> Unit,
    onResumeClick: () -> Unit,
    onFinishClick: () -> Unit,
    onResetClick: () -> Unit,
    modifier: Modifier = Modifier,
    cockpitTiles: List<MetricType> = SettingsRepository.DEFAULT_TILES,
    cockpitTileSources: Map<Int, String> = emptyMap(),
    pairedDevices: List<PairedBleDevice> = emptyList(),
    onTileConfigured: (slotIndex: Int, metricType: MetricType, sourceId: String?) -> Unit = { _, _, _ -> }
) {
    var customizingTileIndex by remember { mutableStateOf<Int?>(null) }

    customizingTileIndex?.let { slotIndex ->
        val currentMetric = cockpitTiles.getOrElse(slotIndex) { MetricType.CADENCE }
        val currentSource = cockpitTileSources[slotIndex]
        TileCustomizationSheet(
            slotIndex = slotIndex,
            currentMetric = currentMetric,
            currentSourceId = currentSource,
            pairedDevices = pairedDevices,
            onSave = { metric, sourceId ->
                onTileConfigured(slotIndex, metric, sourceId)
                customizingTileIndex = null
            },
            onDismiss = {
                customizingTileIndex = null
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            // Extra safe top padding to guarantee clearing the punch-hole camera
            .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 8.dp)
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 1. TOP SECTION: Connection Pills above HR Capsules (Clear of hole-punch)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            TopConnectionPills(
                isGpsActive = telemetry.isGpsActive,
                isHeartRateConnected = telemetry.isHeartRateConnected,
                isBleCadenceConnected = telemetry.isBleCadenceConnected,
                isBleSpeedConnected = telemetry.isBleSpeedConnected,
                isBlePowerConnected = telemetry.isBlePowerConnected
            )

            Spacer(modifier = Modifier.height(14.dp))

            HeartRateZoneCapsules(
                heartRateBpm = telemetry.heartRateBpm,
                zone = telemetry.heartRateZone
            )
        }

        // 2. CENTER SECTION: Adaptive Roboto Flex Speedometer
        SpeedDisplay(
            speedValue = telemetry.speedKmh,
            unitSystem = unitSystem,
            modifier = Modifier.weight(1f)
        )

        // 3. BOTTOM SECTION: 3x2 Metrics Grid & Action Controls
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            BottomMetricsGrid(
                telemetry = telemetry,
                unitSystem = unitSystem,
                tiles = cockpitTiles,
                onTileLongClick = { slotIndex ->
                    customizingTileIndex = slotIndex
                },
                modifier = Modifier.padding(bottom = 16.dp)
            )

            BottomActionControls(
                rideStatus = rideStatus,
                onStartClick = onStartClick,
                onPauseClick = onPauseClick,
                onResumeClick = onResumeClick,
                onFinishClick = onFinishClick,
                onResetClick = onResetClick
            )

            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}
