package com.velo.app.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.velo.app.data.SettingsRepository
import com.velo.app.model.RideStatus
import com.velo.app.model.RideTelemetry
import com.velo.app.model.UnitSystem
import com.velo.app.sensors.MockRideSimulator
import com.velo.app.service.RideTrackingService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RideViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)
    private val simulator = MockRideSimulator(viewModelScope)

    val unitSystem: StateFlow<UnitSystem> = settingsRepository.unitSystemFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UnitSystem.METRIC)

    val isMockSimulatorEnabled: StateFlow<Boolean> = settingsRepository.isMockSimulatorEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true) // Default enabled for instant testing

    val isKeepScreenOn: StateFlow<Boolean> = settingsRepository.isKeepScreenOnEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private val _rideStatus = MutableStateFlow(RideStatus.RECORDING)
    val rideStatus: StateFlow<RideStatus> = _rideStatus.asStateFlow()

    // Live Telemetry stream from simulator or sensors
    val telemetry: StateFlow<RideTelemetry> = simulator.telemetry

    init {
        // Automatically start the simulator if enabled
        viewModelScope.launch {
            isMockSimulatorEnabled.collect { enabled ->
                if (enabled && _rideStatus.value == RideStatus.RECORDING) {
                    simulator.start()
                } else {
                    simulator.stop()
                }
            }
        }
    }

    fun onPauseClick() {
        _rideStatus.value = RideStatus.PAUSED
        simulator.stop()
        sendServiceAction(RideTrackingService.ACTION_PAUSE)
    }

    fun onResumeClick() {
        _rideStatus.value = RideStatus.RECORDING
        if (isMockSimulatorEnabled.value) {
            simulator.start()
        }
        sendServiceAction(RideTrackingService.ACTION_RESUME)
    }

    fun onFinishClick() {
        _rideStatus.value = RideStatus.FINISHED
        simulator.stop()
        sendServiceAction(RideTrackingService.ACTION_STOP)
    }

    fun onUnitSystemChange(unitSystem: UnitSystem) {
        viewModelScope.launch {
            settingsRepository.setUnitSystem(unitSystem)
        }
    }

    fun onMockSimulatorChange(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setMockSimulatorEnabled(enabled)
            if (enabled && _rideStatus.value == RideStatus.RECORDING) {
                simulator.start()
            } else {
                simulator.stop()
            }
        }
    }

    fun onKeepScreenOnChange(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setKeepScreenOnEnabled(enabled)
        }
    }

    private fun sendServiceAction(action: String) {
        val intent = Intent(getApplication(), RideTrackingService::class.java).apply {
            this.action = action
        }
        try {
            getApplication<Application>().startService(intent)
        } catch (e: Exception) {
            // In Android 14+ background launch limitations are caught gracefully
        }
    }
}
