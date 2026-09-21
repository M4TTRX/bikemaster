package com.velo.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.velo.app.ui.MainScreen
import com.velo.app.ui.theme.VeloTheme
import com.velo.app.viewmodel.RideViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: RideViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            VeloTheme {
                val telemetry by viewModel.telemetry.collectAsState()
                val unitSystem by viewModel.unitSystem.collectAsState()
                val rideStatus by viewModel.rideStatus.collectAsState()
                val isMockSimulatorEnabled by viewModel.isMockSimulatorEnabled.collectAsState()
                val isKeepScreenOn by viewModel.isKeepScreenOn.collectAsState()

                if (isKeepScreenOn) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }

                MainScreen(
                    telemetry = telemetry,
                    unitSystem = unitSystem,
                    rideStatus = rideStatus,
                    isMockSimulatorEnabled = isMockSimulatorEnabled,
                    isKeepScreenOn = isKeepScreenOn,
                    onUnitSystemChange = viewModel::onUnitSystemChange,
                    onMockSimulatorChange = viewModel::onMockSimulatorChange,
                    onKeepScreenOnChange = viewModel::onKeepScreenOnChange,
                    onPauseClick = viewModel::onPauseClick,
                    onResumeClick = viewModel::onResumeClick,
                    onFinishClick = viewModel::onFinishClick
                )
            }
        }
    }
}
