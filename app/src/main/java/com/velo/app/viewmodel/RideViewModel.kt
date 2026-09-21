package com.velo.app.viewmodel

import android.app.Application
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.velo.app.VeloApplication
import com.velo.app.data.SettingsRepository
import com.velo.app.model.RideStatus
import com.velo.app.model.RideTelemetry
import com.velo.app.model.UnitSystem
import com.velo.app.service.RideTrackingService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RideViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)
    private val rideRepository = (application as VeloApplication).rideRepository

    val unitSystem: StateFlow<UnitSystem> = settingsRepository.unitSystemFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UnitSystem.METRIC)

    val isMockSimulatorEnabled: StateFlow<Boolean> = settingsRepository.isMockSimulatorEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isKeepScreenOn: StateFlow<Boolean> = settingsRepository.isKeepScreenOnEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val rideStatus: StateFlow<RideStatus> = rideRepository.rideStatus
    val telemetry: StateFlow<RideTelemetry> = rideRepository.telemetry

    init {
        // Automatically start the foreground service on initial launch
        sendServiceAction(RideTrackingService.ACTION_START)
    }

    fun onPauseClick() {
        rideRepository.pauseRide()
        sendServiceAction(RideTrackingService.ACTION_PAUSE)
    }

    fun onResumeClick() {
        rideRepository.resumeRide(isMockSimulatorEnabled.value)
        sendServiceAction(RideTrackingService.ACTION_RESUME)
    }

    fun onFinishClick() {
        rideRepository.finishRide()
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
            if (rideStatus.value == RideStatus.RECORDING) {
                rideRepository.startRide(enabled)
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
            if (action == RideTrackingService.ACTION_START) {
                ContextCompat.startForegroundService(getApplication(), intent)
            } else {
                getApplication<Application>().startService(intent)
            }
        } catch (e: Exception) {
            // Guard against background start restrictions
        }
    }
}
