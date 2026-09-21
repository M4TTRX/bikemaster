package com.velo.app.data

import android.location.Location
import com.velo.app.model.RideStatus
import com.velo.app.model.RideTelemetry
import com.velo.app.sensors.BleSensorManager
import com.velo.app.sensors.DiscoveredSensor
import com.velo.app.sensors.LocationTracker
import com.velo.app.sensors.MockRideSimulator
import com.velo.app.sensors.SensorType
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

interface RideRepository {
    val telemetry: StateFlow<RideTelemetry>
    val rideStatus: StateFlow<RideStatus>
    val discoveredSensors: StateFlow<List<DiscoveredSensor>>
    val isScanning: StateFlow<Boolean>
    fun startRide(useSimulator: Boolean)
    fun pauseRide()
    fun resumeRide(useSimulator: Boolean)
    fun finishRide()
    fun startSensorScan(type: SensorType)
    fun stopSensorScan()
    fun pairSensor(sensor: DiscoveredSensor)
}

class DefaultRideRepository(
    private val locationTracker: LocationTracker,
    private val bleSensorManager: BleSensorManager,
    private val wearOsManager: WearOsManager,
    private val settingsRepository: SettingsRepository? = null
) : RideRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val simulator = MockRideSimulator(scope)

    private val _rideStatus = MutableStateFlow(RideStatus.RECORDING)
    override val rideStatus: StateFlow<RideStatus> = _rideStatus.asStateFlow()

    private val _realTelemetry = MutableStateFlow(
        RideTelemetry(
            speedKmh = 0f,
            heartRateBpm = 0,
            distanceMeters = 0.0,
            elapsedTimeSeconds = 0L,
            cadenceRpm = 0,
            avgSpeedKmh = 0f,
            elevationGainMeters = 0f,
            powerWatts = 0,
            isGpsActive = false,
            isWearOsConnected = false
        )
    )

    private val _useSimulator = MutableStateFlow(true)
    private val _pairedSensors = MutableStateFlow<Set<SensorType>>(emptySet())

    private var locationJob: Job? = null
    private var timerJob: Job? = null
    private var lastLocation: Location? = null

    override val telemetry: StateFlow<RideTelemetry> = combine(
        _useSimulator,
        simulator.telemetry,
        _realTelemetry,
        _pairedSensors
    ) { useSim, simData, realData, paired ->
        val base = if (useSim) simData else realData
        base.copy(
            isBleCadenceConnected = paired.contains(SensorType.CADENCE_SPEED) || base.isBleCadenceConnected,
            isBlePowerConnected = paired.contains(SensorType.POWER) || base.isBlePowerConnected,
            isWearOsConnected = paired.contains(SensorType.HEART_RATE) || base.isWearOsConnected
        )
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = simulator.telemetry.value
    )

    init {
        // Collect BLE cadence & power
        scope.launch {
            combine(bleSensorManager.cadenceFlow, bleSensorManager.powerFlow) { cad, pwr ->
                cad to pwr
            }.collect { (cad, pwr) ->
                if (!_useSimulator.value) {
                    _realTelemetry.update {
                        it.copy(
                            cadenceRpm = cad,
                            powerWatts = pwr,
                            isBleCadenceConnected = cad > 0,
                            isBlePowerConnected = pwr > 0
                        )
                    }
                }
            }
        }

        // Collect Wear OS heart rate
        scope.launch {
            wearOsManager.wearOsHeartRate.collect { hr ->
                if (!_useSimulator.value) {
                    _realTelemetry.update {
                        it.copy(
                            heartRateBpm = hr,
                            isWearOsConnected = hr > 0
                        )
                    }
                }
            }
        }

        // Restore paired sensors from settings DataStore
        if (settingsRepository != null) {
            scope.launch {
                settingsRepository.pairedSensorsFlow.collect { savedSet ->
                    _pairedSensors.value = savedSet
                }
            }
        }

        // Default start simulator for out-of-the-box emulator experience
        simulator.start()
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
                        cur.copy(
                            elapsedTimeSeconds = elapsed,
                            avgSpeedKmh = avgSpeed
                        )
                    }
                }
            }
        }

        // Battery-saving GPS collection: ONLY active while RECORDING
        locationJob = scope.launch {
            locationTracker.getLocationFlow().collect { location ->
                if (_rideStatus.value != RideStatus.RECORDING) return@collect
                val prev = lastLocation
                val speedKmh = (location.speed * 3.6f).coerceAtLeast(0f)
                var addedDistance = 0.0
                var addedElevation = 0f

                if (prev != null && (!location.hasAccuracy() || location.accuracy < 25f)) {
                    val deltaDist = prev.distanceTo(location).toDouble()
                    // Filter stationary GPS jitter
                    if (deltaDist > 1.5 && speedKmh > 1.5f) {
                        addedDistance = deltaDist
                    }
                    if (location.hasAltitude() && prev.hasAltitude()) {
                        val deltaAlt = (location.altitude - prev.altitude).toFloat()
                        if (deltaAlt > 0.5f) addedElevation = deltaAlt
                    }
                }
                lastLocation = location

                _realTelemetry.update { cur ->
                    val newDist = cur.distanceMeters + addedDistance
                    cur.copy(
                        speedKmh = speedKmh,
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
        timerJob?.cancel()
        timerJob = null
        lastLocation = null
    }

    private val _discoveredSensors = MutableStateFlow<List<DiscoveredSensor>>(emptyList())
    override val discoveredSensors: StateFlow<List<DiscoveredSensor>> = _discoveredSensors.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    override val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    override fun startRide(useSimulator: Boolean) {
        _useSimulator.value = useSimulator
        _rideStatus.value = RideStatus.RECORDING
        if (useSimulator) {
            stopTrackingRealData()
            simulator.start()
        } else {
            simulator.stop()
            wearOsManager.startListening()
            startTrackingRealData()
        }
    }

    override fun pauseRide() {
        _rideStatus.value = RideStatus.PAUSED
        simulator.stop()
        stopTrackingRealData()
    }

    override fun resumeRide(useSimulator: Boolean) {
        _useSimulator.value = useSimulator
        _rideStatus.value = RideStatus.RECORDING
        if (useSimulator) {
            stopTrackingRealData()
            simulator.start()
        } else {
            simulator.stop()
            wearOsManager.startListening()
            startTrackingRealData()
        }
    }

    override fun finishRide() {
        _rideStatus.value = RideStatus.FINISHED
        simulator.stop()
        wearOsManager.stopListening()
        stopTrackingRealData()
    }

    override fun startSensorScan(type: SensorType) {
        _isScanning.value = true
        scope.launch {
            // Emulate device discovery for both emulator testing and BLE scanning
            kotlinx.coroutines.delay(1200L)
            val demoSensors = when (type) {
                SensorType.CADENCE_SPEED -> listOf(
                    DiscoveredSensor("Wahoo SPEED v2", "C4:7D:CC:12:34:56", -62, type),
                    DiscoveredSensor("Garmin CADENCE 2", "D1:9A:88:AB:CD:EF", -74, type),
                    DiscoveredSensor("Magene S3+", "E0:44:11:22:33:44", -81, type)
                )
                SensorType.POWER -> listOf(
                    DiscoveredSensor("Assioma DUO-L", "F2:15:AA:77:88:99", -58, type),
                    DiscoveredSensor("Stages Power R", "A0:BC:33:44:55:66", -70, type),
                    DiscoveredSensor("4iiii Precision 3", "B5:22:99:88:77:66", -79, type)
                )
                SensorType.HEART_RATE -> listOf(
                    DiscoveredSensor("Polar H10 9A28B1", "00:22:D0:9A:28:B1", -55, type),
                    DiscoveredSensor("Garmin HRM-Pro", "C8:FD:19:55:44:33", -68, type),
                    DiscoveredSensor("Wahoo TICKR", "E4:5F:01:23:45:67", -76, type)
                )
            }
            _discoveredSensors.value = demoSensors
            _isScanning.value = false
        }
    }

    override fun stopSensorScan() {
        _isScanning.value = false
    }

    override fun pairSensor(sensor: DiscoveredSensor) {
        _pairedSensors.update { it + sensor.type }
        _realTelemetry.update { current ->
            when (sensor.type) {
                SensorType.CADENCE_SPEED -> current.copy(isBleCadenceConnected = true)
                SensorType.POWER -> current.copy(isBlePowerConnected = true)
                SensorType.HEART_RATE -> current.copy(isWearOsConnected = true)
            }
        }
        if (settingsRepository != null) {
            scope.launch {
                settingsRepository.pairSensor(sensor.type)
            }
        }
        stopSensorScan()
    }
}
