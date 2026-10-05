package com.velo.app.viewmodel

import android.app.Application
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.velo.app.VeloApplication
import com.velo.app.data.PairedBleDevice
import com.velo.app.data.SettingsRepository
import com.velo.app.model.MetricType
import com.velo.app.model.RideStatus
import com.velo.app.model.RideTelemetry
import com.velo.app.model.SensorDataType
import com.velo.app.model.SpeedSource
import com.velo.app.model.ThemeMode
import com.velo.app.model.UnitSystem
import com.velo.app.sensors.DiscoveredSensor
import com.velo.app.service.RideTrackingService
import com.velo.app.util.isRunningOnEmulator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RideViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)
    private val rideRepository = (application as VeloApplication).rideRepository

    val isEmulator: Boolean = isRunningOnEmulator()

    val themeMode: StateFlow<ThemeMode> = settingsRepository.themeModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.DARK)

    val unitSystem: StateFlow<UnitSystem> = settingsRepository.unitSystemFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), settingsRepository.defaultUnitSystem)

    val speedSource: StateFlow<SpeedSource> = settingsRepository.speedSourceFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SpeedSource.AUTO)

    val wheelCircumferenceMm: StateFlow<Int> = settingsRepository.wheelCircumferenceFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2105)

    val pairedBleDevices: StateFlow<List<PairedBleDevice>> = settingsRepository.pairedBleDevicesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cockpitTiles: StateFlow<List<MetricType>> = settingsRepository.cockpitTilesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsRepository.DEFAULT_TILES)

    val cockpitTileSources: StateFlow<Map<Int, String>> = settingsRepository.cockpitTileSourcesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val isMockSimulatorEnabled: StateFlow<Boolean> = settingsRepository.isMockSimulatorEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isKeepScreenOn: StateFlow<Boolean> = settingsRepository.isKeepScreenOnEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val rideStatus: StateFlow<RideStatus> = rideRepository.rideStatus
    val telemetry: StateFlow<RideTelemetry> = rideRepository.telemetry
    val discoveredSensors: StateFlow<List<DiscoveredSensor>> = rideRepository.discoveredSensors
    val isScanning: StateFlow<Boolean> = rideRepository.isScanning
    val isWearOsCompanionInstalled: StateFlow<Boolean> = rideRepository.isWearOsCompanionInstalled
    val isWearOsConnected: StateFlow<Boolean> = rideRepository.isWearOsConnected

    fun onInstallWearOsCompanion() {
        rideRepository.installWearOsCompanion()
    }

    fun startSensorScan() {
        rideRepository.startSensorScan()
    }

    fun stopSensorScan() {
        rideRepository.stopSensorScan()
    }

    fun pairSensor(sensor: DiscoveredSensor) {
        rideRepository.pairSensor(sensor)
    }

    fun updateSensorEnabledTypes(address: String, types: Set<SensorDataType>) {
        rideRepository.updateSensorEnabledTypes(address, types)
    }

    fun unpairSensor(address: String) {
        rideRepository.unpairSensor(address)
    }

    fun onTileConfigured(slotIndex: Int, metricType: MetricType, sourceId: String?) {
        viewModelScope.launch {
            settingsRepository.setCockpitTile(slotIndex, metricType, sourceId)
        }
    }

    fun onSpeedSourceChange(speedSource: SpeedSource) {
        viewModelScope.launch {
            settingsRepository.setSpeedSource(speedSource)
        }
    }

    fun onWheelCircumferenceChange(circumferenceMm: Int) {
        viewModelScope.launch {
            settingsRepository.setWheelCircumference(circumferenceMm)
        }
    }

    fun onStartClick() {
        // Only run simulator if on emulator AND mock switch is enabled
        val useSim = isEmulator && isMockSimulatorEnabled.value
        rideRepository.startRide(useSim)
        sendServiceAction(RideTrackingService.ACTION_START)
    }

    fun onPauseClick() {
        rideRepository.pauseRide()
        sendServiceAction(RideTrackingService.ACTION_PAUSE)
    }

    fun onResumeClick() {
        val useSim = isEmulator && isMockSimulatorEnabled.value
        rideRepository.resumeRide(useSim)
        sendServiceAction(RideTrackingService.ACTION_RESUME)
    }

    fun onFinishClick() {
        rideRepository.finishRide()
        sendServiceAction(RideTrackingService.ACTION_STOP)
    }

    fun onResetClick() {
        rideRepository.resetRide()
        sendServiceAction(RideTrackingService.ACTION_STOP)
    }

    fun onThemeModeChange(themeMode: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.setThemeMode(themeMode)
        }
    }

    fun onUnitSystemChange(unitSystem: UnitSystem) {
        viewModelScope.launch {
            settingsRepository.setUnitSystem(unitSystem)
        }
    }

    fun onMockSimulatorChange(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setMockSimulatorEnabled(enabled)
            if (rideStatus.value == RideStatus.RECORDING && isEmulator) {
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
