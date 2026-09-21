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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.velo.app.model.RideStatus
import com.velo.app.model.RideTelemetry
import com.velo.app.model.UnitSystem
import com.velo.app.ui.components.BottomActionControls
import com.velo.app.ui.components.BottomMetricsGrid
import com.velo.app.ui.components.HeartRateZoneCapsules
import com.velo.app.ui.components.SpeedDisplay
import com.velo.app.ui.components.TopConnectionPills
import com.velo.app.ui.theme.OledBlack

@Composable
fun RideDashboardScreen(
    telemetry: RideTelemetry,
    unitSystem: UnitSystem,
    rideStatus: RideStatus,
    onPauseClick: () -> Unit,
    onResumeClick: () -> Unit,
    onFinishClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(OledBlack)
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
                isWearOsConnected = telemetry.isWearOsConnected
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
                modifier = Modifier.padding(bottom = 16.dp)
            )

            BottomActionControls(
                rideStatus = rideStatus,
                onPauseClick = onPauseClick,
                onResumeClick = onResumeClick,
                onFinishClick = onFinishClick
            )

            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}
