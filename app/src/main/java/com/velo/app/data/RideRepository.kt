package com.velo.app.data

import android.location.Location
import com.velo.app.model.RideStatus
import com.velo.app.model.RideTelemetry
import com.velo.app.model.SensorDataType
import com.velo.app.model.SpeedSource
import com.velo.app.sensors.BleSensorManager
import com.velo.app.sensors.DiscoveredSensor
import com.velo.app.sensors.LocationTracker
import com.velo.app.sensors.MockRideSimulator
import com.velo.app.sensors.WearOsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.max

interface RideRepository {
    val telemetry: StateFlow<RideTelemetry>
    val rideStatus: StateFlow<RideStatus>
    val discoveredSensors: StateFlow<List<DiscoveredSensor>>
    val isScanning: StateFlow<Boolean>
    val isWearOsCompanionInstalled: StateFlow<Boolean>
    val isWearOsConnected: StateFlow<Boolean>
    fun startRide(useSimulator: Boolean)
    fun pauseRide()
    fun resumeRide(useSimulator: Boolean)
    fun finishRide()
    fun resetRide()
    fun startSensorScan()
    fun stopSensorScan()
    fun pairSensor(sensor: DiscoveredSensor)
    fun updateSensorEnabledTypes(address: String, types: Set<SensorDataType>)
    fun unpairSensor(address: String)
    fun disconnectSensor(address: String)
    fun installWearOsCompanion()
}

class DefaultRideRepository(
    private val locationTracker: LocationTracker,
    private val bleSensorManager: BleSensorManager,
    private val wearOsManager: WearOsManager,
    private val settingsRepository: SettingsRepository? = null
) : RideRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val simulator = MockRideSimulator(scope)

    private val _rideStatus = MutableStateFlow(RideStatus.IDLE)
    override val rideStatus: StateFlow<RideStatus> = _rideStatus.asStateFlow()
    override val isWearOsCompanionInstalled: StateFlow<Boolean> = wearOsManager.isCompanionInstalled
    override val isWearOsConnected: StateFlow<Boolean> = wearOsManager.isConnected

    private val _speedSource = MutableStateFlow(SpeedSource.AUTO)

    private val _realTelemetry = MutableStateFlow(
        RideTelemetry(
            speedKmh = 0f,
            maxSpeedKmh = 0f,
            heartRateBpm = 0,
            distanceMeters = 0.0,
            elapsedTimeSeconds = 0L,
            cadenceRpm = 0,
            avgSpeedKmh = 0f,
            elevationGainMeters = 0f,
            powerWatts = 0,
            avgPowerWatts = 0,
            maxPowerWatts = 0,
            isGpsActive = false,
            isHeartRateConnected = false,
            isBleCadenceConnected = false,
            isBleSpeedConnected = false,
            isBlePowerConnected = false,
            speedSource = SpeedSource.AUTO
        )
    )

    private val _useSimulator = MutableStateFlow(false)
    private var totalPowerJoules: Long = 0L

    private var locationJob: Job? = null
    private var speedSensorJob: Job? = null
    private var timerJob: Job? = null
    private var lastLocation: Location? = null

    override val discoveredSensors: StateFlow<List<DiscoveredSensor>> = bleSensorManager.discoveredSensors
    override val isScanning: StateFlow<Boolean> = bleSensorManager.isScanning

    private val baseFlow = combine(
        _useSimulator,
        simulator.telemetry,
        _realTelemetry,
        _speedSource
    ) { useSim, simData, realData, speedSrc ->
        val base = if (useSim) simData else realData
        base.copy(speedSource = speedSrc)
    }

    override val telemetry: StateFlow<RideTelemetry> = combine(
        baseFlow,
        bleSensorManager.isCadenceConnected,
        bleSensorManager.isSpeedConnected,
        bleSensorManager.isPowerConnected,
        combine(
            bleSensorManager.isHeartRateConnected,
            wearOsManager.isConnected,
            wearOsManager.wearOsHeartRate
        ) { bleHr, wearConn, hr ->
            bleHr || wearConn || hr > 0
        }
    ) { base, bleCadConn, bleSpeedConn, blePowConn, hrConn ->
        base.copy(
            isBleCadenceConnected = bleCadConn || base.isBleCadenceConnected,
            isBleSpeedConnected = bleSpeedConn || base.isBleSpeedConnected,
            isBlePowerConnected = blePowConn || base.isBlePowerConnected,
            isHeartRateConnected = hrConn || base.isHeartRateConnected
        )
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = _realTelemetry.value
    )

    init {
        wearOsManager.startListening()
        wearOsManager.sendRideStatus(_rideStatus.value.name)

        // Collect BLE cadence
        scope.launch {
            bleSensorManager.cadenceFlow.collect { cad ->
                if (!_useSimulator.value) {
                    _realTelemetry.update {
                        it.copy(cadenceRpm = cad)
                    }
                }
            }
        }

        // Collect BLE power
        scope.launch {
            bleSensorManager.powerFlow.collect { watts ->
                if (!_useSimulator.value) {
                    if (_rideStatus.value == RideStatus.RECORDING && watts > 0) {
                        totalPowerJoules += watts
                    }
                    _realTelemetry.update { cur ->
                        val newMax = max(cur.maxPowerWatts, watts)
                        val newAvg = if (cur.elapsedTimeSeconds > 0) (totalPowerJoules / cur.elapsedTimeSeconds).toInt() else 0
                        cur.copy(
                            powerWatts = watts,
                            maxPowerWatts = newMax,
                            avgPowerWatts = newAvg
                        )
                    }
                }
            }
        }

        // Collect BLE & Wear OS heart rate
        scope.launch {
            combine(bleSensorManager.heartRateFlow, wearOsManager.wearOsHeartRate) { bleHr, wearHr ->
                if (bleHr > 0) bleHr else wearHr
            }.collect { hr ->
                if (!_useSimulator.value) {
                    _realTelemetry.update {
                        it.copy(heartRateBpm = hr)
                    }
                }
            }
        }

        // Collect live BLE speed (for live speedometer display when idle or paused)
        scope.launch {
            bleSensorManager.speedFlow.collect { speedKmh ->
                if (!_useSimulator.value && shouldUseSpeedSensor()) {
                    if (_rideStatus.value != RideStatus.RECORDING) {
                        _realTelemetry.update { it.copy(speedKmh = speedKmh) }
                    }
                }
            }
        }

        // Settings observation (speed source, wheel circumference, paired devices, simulator)
        if (settingsRepository != null) {
            scope.launch {
                settingsRepository.isMockSimulatorEnabled.collect { mockEnabled ->
                    _useSimulator.value = mockEnabled
                }
            }

            scope.launch {
                settingsRepository.speedSourceFlow.collect { source ->
                    _speedSource.value = source
                    _realTelemetry.update { it.copy(speedSource = source) }
                }
            }

            scope.launch {
                settingsRepository.wheelCircumferenceFlow.collect { circumferenceMm ->
                    bleSensorManager.setWheelCircumference(circumferenceMm)
                }
            }

            scope.launch {
                settingsRepository.pairedBleDevicesFlow.collect { savedDevices ->
                    savedDevices.forEach { device ->
                        bleSensorManager.connectDevice(device.address, device.name, device.enabledDataTypes)
                    }
                }
            }

            bleSensorManager.onCapabilitiesResolved = { address, detected, enabled ->
                scope.launch {
                    settingsRepository.updateSensorCapabilities(address, detected, enabled)
                }
            }
        }
    }

    private fun shouldUseSpeedSensor(): Boolean {
        val src = _speedSource.value
        val hasSensor = bleSensorManager.isSpeedConnected.value
        return when (src) {
            SpeedSource.SPEED_SENSOR -> true
            SpeedSource.AUTO -> hasSensor
            SpeedSource.GPS -> false
        }
    }

    private fun startTrackingRealData() {
        stopTrackingRealData()
        lastLocation = null

        // 1Hz accurate clock
        timerJob = scope.launch {
            while (isActive) {
                delay(1000L)
                if (_rideStatus.value == RideStatus.RECORDING) {
                    _realTelemetry.update { cur ->
                        val elapsed = cur.elapsedTimeSeconds + 1
                        val avgSpeed = if (elapsed > 0) ((cur.distanceMeters / 1000.0) / (elapsed / 3600.0)).toFloat() else 0f
                        val avgPower = if (elapsed > 0) (totalPowerJoules / elapsed).toInt() else 0
                        cur.copy(
                            elapsedTimeSeconds = elapsed,
                            avgSpeedKmh = avgSpeed,
                            avgPowerWatts = avgPower
                        )
                    }
                }
            }
        }

        // Wheel sensor speed & distance accumulation: ONLY while RECORDING
        speedSensorJob = scope.launch {
            combine(bleSensorManager.speedFlow, bleSensorManager.distanceDeltaFlow) { speed, deltaDist ->
                Pair(speed, deltaDist)
            }.collect { (speedKmh, deltaDist) ->
                if (_rideStatus.value != RideStatus.RECORDING) return@collect
                if (!shouldUseSpeedSensor()) return@collect

                _realTelemetry.update { cur ->
                    val newDist = cur.distanceMeters + deltaDist
                    val newMax = max(cur.maxSpeedKmh, speedKmh)
                    cur.copy(
                        speedKmh = speedKmh,
                        maxSpeedKmh = newMax,
                        distanceMeters = newDist
                    )
                }
            }
        }

        // GPS collection: ONLY active while RECORDING
        locationJob = scope.launch {
            locationTracker.getLocationFlow().collect { location ->
                if (_rideStatus.value != RideStatus.RECORDING) return@collect
                val prev = lastLocation
                val gpsSpeedKmh = (location.speed * 3.6f).coerceAtLeast(0f)
                var addedDistance = 0.0
                var addedElevation = 0f

                if (prev != null && (!location.hasAccuracy() || location.accuracy < 25f)) {
                    val deltaDist = prev.distanceTo(location).toDouble()
                    // Filter stationary GPS jitter
                    if (deltaDist > 1.5 && gpsSpeedKmh > 1.5f) {
                        addedDistance = deltaDist
                    }
                    if (location.hasAltitude() && prev.hasAltitude()) {
                        val deltaAlt = (location.altitude - prev.altitude).toFloat()
                        if (deltaAlt > 0.5f) addedElevation = deltaAlt
                    }
                }
                lastLocation = location

                val useSensor = shouldUseSpeedSensor()

                _realTelemetry.update { cur ->
                    val newDist = if (useSensor) cur.distanceMeters else cur.distanceMeters + addedDistance
                    val activeSpeed = if (useSensor) cur.speedKmh else gpsSpeedKmh
                    val newMax = max(cur.maxSpeedKmh, activeSpeed)
                    cur.copy(
                        speedKmh = activeSpeed,
                        maxSpeedKmh = newMax,
                        distanceMeters = newDist,
                        elevationGainMeters = cur.elevationGainMeters + addedElevation,
                        isGpsActive = true
                    )
                }
            }
        }
    }

    private fun stopTrackingRealData() {
        locationJob?.cancel()
        locationJob = null
        speedSensorJob?.cancel()
        speedSensorJob = null
        timerJob?.cancel()
        timerJob = null
        lastLocation = null
    }

    override fun startRide(useSimulator: Boolean) {
        _useSimulator.value = useSimulator
        _rideStatus.value = RideStatus.RECORDING
        wearOsManager.startListening()
        wearOsManager.openCompanionApp()
        wearOsManager.sendRideStatus("RECORDING")
        if (useSimulator) {
            stopTrackingRealData()
            simulator.start()
        } else {
            simulator.stop()
            startTrackingRealData()
        }
    }

    override fun pauseRide() {
        _rideStatus.value = RideStatus.PAUSED
        wearOsManager.sendRideStatus("PAUSED")
        simulator.stop()
        stopTrackingRealData()
    }

    override fun resumeRide(useSimulator: Boolean) {
        _useSimulator.value = useSimulator
        _rideStatus.value = RideStatus.RECORDING
        wearOsManager.startListening()
        wearOsManager.openCompanionApp()
        wearOsManager.sendRideStatus("RECORDING")
        if (useSimulator) {
            stopTrackingRealData()
            simulator.start()
        } else {
            simulator.stop()
            startTrackingRealData()
        }
    }

    override fun finishRide() {
        _rideStatus.value = RideStatus.FINISHED
        wearOsManager.sendRideStatus("FINISHED")
        simulator.stop()
        stopTrackingRealData()
    }

    override fun resetRide() {
        stopTrackingRealData()
        simulator.stop()
        simulator.reset()
        wearOsManager.sendRideStatus("IDLE")
        wearOsManager.startListening()
        totalPowerJoules = 0L
        _rideStatus.value = RideStatus.IDLE
        _realTelemetry.value = RideTelemetry(
            speedKmh = 0f,
            maxSpeedKmh = 0f,
            heartRateBpm = 0,
            distanceMeters = 0.0,
            elapsedTimeSeconds = 0L,
            cadenceRpm = 0,
            avgSpeedKmh = 0f,
            elevationGainMeters = 0f,
            powerWatts = 0,
            avgPowerWatts = 0,
            maxPowerWatts = 0,
            isGpsActive = false,
            isHeartRateConnected = false,
            isBleCadenceConnected = false,
            isBleSpeedConnected = false,
            isBlePowerConnected = false,
            speedSource = _speedSource.value
        )
    }

    override fun startSensorScan() {
        bleSensorManager.startScanning(isMockEnabled = _useSimulator.value)
    }

    override fun stopSensorScan() {
        bleSensorManager.stopScanning()
    }

    override fun pairSensor(sensor: DiscoveredSensor) {
        val defaultEnabled = if (sensor.detectedCapabilities.isNotEmpty()) {
            sensor.detectedCapabilities
        } else {
            // For unknown sensors, enable all standard types until GATT discovery confirms capabilities
            setOf(
                SensorDataType.SPEED,
                SensorDataType.CADENCE,
                SensorDataType.POWER,
                SensorDataType.HEART_RATE
            )
        }

        bleSensorManager.connectDevice(sensor.address, sensor.name, defaultEnabled)

        if (settingsRepository != null) {
            scope.launch {
                settingsRepository.savePairedDevice(
                    PairedBleDevice(
                        address = sensor.address,
                        name = sensor.name,
                        detectedCapabilities = sensor.detectedCapabilities,
                        enabledDataTypes = defaultEnabled
                    )
                )
            }
        }
        stopSensorScan()
    }

    override fun updateSensorEnabledTypes(address: String, types: Set<SensorDataType>) {
        bleSensorManager.updateSensorEnabledTypes(address, types)
        if (settingsRepository != null) {
            scope.launch {
                settingsRepository.updateSensorEnabledTypes(address, types)
            }
        }
    }

    override fun unpairSensor(address: String) {
        bleSensorManager.disconnectDevice(address)
        if (settingsRepository != null) {
            scope.launch {
                settingsRepository.removePairedDevice(address)
            }
        }
    }

    override fun disconnectSensor(address: String) {
        bleSensorManager.disconnectDevice(address)
    }

    override fun installWearOsCompanion() {
        wearOsManager.installCompanionApp()
    }
}
