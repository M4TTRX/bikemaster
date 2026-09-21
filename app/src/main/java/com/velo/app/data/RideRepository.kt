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
    fun startRide(useSimulator: Boolean)
    fun pauseRide()
    fun resumeRide(useSimulator: Boolean)
    fun finishRide()
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
}
