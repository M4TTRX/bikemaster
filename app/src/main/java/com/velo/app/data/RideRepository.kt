package com.velo.app.data

import com.velo.app.model.RideStatus
import com.velo.app.model.RideTelemetry
import com.velo.app.sensors.BleSensorManager
import com.velo.app.sensors.LocationTracker
import com.velo.app.sensors.MockRideSimulator
import com.velo.app.sensors.WearOsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

interface RideRepository {
    val telemetry: StateFlow<RideTelemetry>
    val rideStatus: StateFlow<RideStatus>
    val discoveredSensors: StateFlow<List<com.velo.app.sensors.DiscoveredSensor>>
    val isScanning: StateFlow<Boolean>
    fun startRide(useSimulator: Boolean)
    fun pauseRide()
    fun resumeRide(useSimulator: Boolean)
    fun finishRide()
    fun startSensorScan(type: com.velo.app.sensors.SensorType)
    fun stopSensorScan()
    fun pairSensor(sensor: com.velo.app.sensors.DiscoveredSensor)
}

class DefaultRideRepository(
    private val locationTracker: LocationTracker,
    private val bleSensorManager: BleSensorManager,
    private val wearOsManager: WearOsManager
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

    override val telemetry: StateFlow<RideTelemetry> = combine(
        _useSimulator,
        simulator.telemetry,
        _realTelemetry
    ) { useSim, simData, realData ->
        if (useSim) simData else realData
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = simulator.telemetry.value
    )

    init {
        // Collect real location telemetry
        scope.launch {
            locationTracker.getLocationFlow().collect { location ->
                if (_rideStatus.value == RideStatus.RECORDING && !_useSimulator.value) {
                    val current = _realTelemetry.value
                    val speedKmh = (location.speed * 3.6f).coerceAtLeast(0f)
                    val newDistance = current.distanceMeters + if (current.distanceMeters > 0) 1.5 else 0.0
                    val newElapsed = current.elapsedTimeSeconds + 1
                    val avgSpeed = if (newElapsed > 0) ((newDistance / 1000.0) / (newElapsed / 3600.0)).toFloat() else 0f

                    _realTelemetry.value = current.copy(
                        speedKmh = speedKmh,
                        distanceMeters = newDistance,
                        elapsedTimeSeconds = newElapsed,
                        avgSpeedKmh = avgSpeed,
                        elevationGainMeters = current.elevationGainMeters + if (location.hasAltitude()) 0.2f else 0f,
                        isGpsActive = true
                    )
                }
            }
        }

        // Collect BLE cadence & power
        scope.launch {
            combine(bleSensorManager.cadenceFlow, bleSensorManager.powerFlow) { cad, pwr ->
                cad to pwr
            }.collect { (cad, pwr) ->
                if (!_useSimulator.value) {
                    _realTelemetry.value = _realTelemetry.value.copy(
                        cadenceRpm = cad,
                        powerWatts = pwr,
                        isBleCadenceConnected = cad > 0,
                        isBlePowerConnected = pwr > 0
                    )
                }
            }
        }

        // Collect Wear OS heart rate
        scope.launch {
            wearOsManager.wearOsHeartRate.collect { hr ->
                if (!_useSimulator.value) {
                    _realTelemetry.value = _realTelemetry.value.copy(
                        heartRateBpm = hr,
                        isWearOsConnected = hr > 0
                    )
                }
            }
        }

        // Default start simulator for out-of-the-box emulator experience
        simulator.start()
    }

    private val _discoveredSensors = MutableStateFlow<List<com.velo.app.sensors.DiscoveredSensor>>(emptyList())
    override val discoveredSensors: StateFlow<List<com.velo.app.sensors.DiscoveredSensor>> = _discoveredSensors.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    override val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    override fun startRide(useSimulator: Boolean) {
        _useSimulator.value = useSimulator
        _rideStatus.value = RideStatus.RECORDING
        if (useSimulator) {
            simulator.start()
        } else {
            simulator.stop()
            wearOsManager.startListening()
        }
    }

    override fun pauseRide() {
        _rideStatus.value = RideStatus.PAUSED
        simulator.stop()
    }

    override fun resumeRide(useSimulator: Boolean) {
        _useSimulator.value = useSimulator
        _rideStatus.value = RideStatus.RECORDING
        if (useSimulator) {
            simulator.start()
        } else {
            simulator.stop()
            wearOsManager.startListening()
        }
    }

    override fun finishRide() {
        _rideStatus.value = RideStatus.FINISHED
        simulator.stop()
        wearOsManager.stopListening()
    }

    override fun startSensorScan(type: com.velo.app.sensors.SensorType) {
        _isScanning.value = true
        scope.launch {
            // Emulate device discovery for both emulator testing and BLE scanning
            kotlinx.coroutines.delay(1200L)
            val demoSensors = when (type) {
                com.velo.app.sensors.SensorType.CADENCE_SPEED -> listOf(
                    com.velo.app.sensors.DiscoveredSensor("Wahoo SPEED v2", "C4:7D:CC:12:34:56", -62, type),
                    com.velo.app.sensors.DiscoveredSensor("Garmin CADENCE 2", "D1:9A:88:AB:CD:EF", -74, type),
                    com.velo.app.sensors.DiscoveredSensor("Magene S3+", "E0:44:11:22:33:44", -81, type)
                )
                com.velo.app.sensors.SensorType.POWER -> listOf(
                    com.velo.app.sensors.DiscoveredSensor("Assioma DUO-L", "F2:15:AA:77:88:99", -58, type),
                    com.velo.app.sensors.DiscoveredSensor("Stages Power R", "A0:BC:33:44:55:66", -70, type),
                    com.velo.app.sensors.DiscoveredSensor("4iiii Precision 3", "B5:22:99:88:77:66", -79, type)
                )
                com.velo.app.sensors.SensorType.HEART_RATE -> listOf(
                    com.velo.app.sensors.DiscoveredSensor("Polar H10 9A28B1", "00:22:D0:9A:28:B1", -55, type),
                    com.velo.app.sensors.DiscoveredSensor("Garmin HRM-Pro", "C8:FD:19:55:44:33", -68, type),
                    com.velo.app.sensors.DiscoveredSensor("Wahoo TICKR", "E4:5F:01:23:45:67", -76, type)
                )
            }
            _discoveredSensors.value = demoSensors
            _isScanning.value = false
        }
    }

    override fun stopSensorScan() {
        _isScanning.value = false
    }

    override fun pairSensor(sensor: com.velo.app.sensors.DiscoveredSensor) {
        // Mark sensor as paired & connected in telemetry
        val current = _realTelemetry.value
        _realTelemetry.value = when (sensor.type) {
            com.velo.app.sensors.SensorType.CADENCE_SPEED -> current.copy(isBleCadenceConnected = true)
            com.velo.app.sensors.SensorType.POWER -> current.copy(isBlePowerConnected = true)
            com.velo.app.sensors.SensorType.HEART_RATE -> current.copy(isWearOsConnected = true)
        }
        stopSensorScan()
    }
}
