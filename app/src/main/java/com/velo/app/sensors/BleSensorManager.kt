package com.velo.app.sensors

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class BleSensorManager(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mainHandler = Handler(Looper.getMainLooper())

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter?
        get() = bluetoothManager?.adapter

    private val _cadenceFlow = MutableStateFlow(0)
    val cadenceFlow: StateFlow<Int> = _cadenceFlow.asStateFlow()

    private val _powerFlow = MutableStateFlow(0)
    val powerFlow: StateFlow<Int> = _powerFlow.asStateFlow()

    private val _heartRateFlow = MutableStateFlow(0)
    val heartRateFlow: StateFlow<Int> = _heartRateFlow.asStateFlow()

    private val _isCadenceConnected = MutableStateFlow(false)
    val isCadenceConnected: StateFlow<Boolean> = _isCadenceConnected.asStateFlow()

    private val _isPowerConnected = MutableStateFlow(false)
    val isPowerConnected: StateFlow<Boolean> = _isPowerConnected.asStateFlow()

    private val _isHeartRateConnected = MutableStateFlow(false)
    val isHeartRateConnected: StateFlow<Boolean> = _isHeartRateConnected.asStateFlow()

    private val _discoveredSensors = MutableStateFlow<List<DiscoveredSensor>>(emptyList())
    val discoveredSensors: StateFlow<List<DiscoveredSensor>> = _discoveredSensors.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val activeClients = ConcurrentHashMap<String, SingleSensorBleManager>()
    private var scanCallback: ScanCallback? = null
    private var scanTimeoutJob: Job? = null
    private var currentScanType: SensorType? = null

    private fun hasScanPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun hasConnectPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun startScanning(type: SensorType, isMockEnabled: Boolean = false) {
        stopScanning()
        currentScanType = type
        _discoveredSensors.value = emptyList()
        _isScanning.value = true

        val adapter = bluetoothAdapter
        val scanner: BluetoothLeScanner? = if (adapter?.isEnabled == true && hasScanPermission()) {
            adapter.bluetoothLeScanner
        } else {
            null
        }

        if (scanner != null) {
            val callback = object : ScanCallback() {
                override fun onScanResult(callbackType: Int, result: ScanResult) {
                    val device = result.device
                    val rawName = try {
                        if (hasConnectPermission()) device.name else null
                    } catch (e: SecurityException) {
                        null
                    } ?: result.scanRecord?.deviceName

                    val address = device.address
                    val rssi = result.rssi
                    val serviceUuids = result.scanRecord?.serviceUuids?.map { it.uuid } ?: emptyList()

                    val matchesType = when (type) {
                        SensorType.CADENCE_SPEED -> serviceUuids.contains(SingleSensorBleManager.CSC_SERVICE_UUID) ||
                                rawName?.contains("speed", ignoreCase = true) == true ||
                                rawName?.contains("cadence", ignoreCase = true) == true ||
                                rawName?.contains("csc", ignoreCase = true) == true ||
                                rawName?.contains("magene", ignoreCase = true) == true ||
                                rawName?.contains("wahoo", ignoreCase = true) == true ||
                                rawName?.contains("garmin", ignoreCase = true) == true ||
                                rawName?.contains("coospo", ignoreCase = true) == true
                        SensorType.POWER -> serviceUuids.contains(SingleSensorBleManager.POWER_SERVICE_UUID) ||
                                rawName?.contains("power", ignoreCase = true) == true ||
                                rawName?.contains("assioma", ignoreCase = true) == true ||
                                rawName?.contains("stages", ignoreCase = true) == true ||
                                rawName?.contains("4iiii", ignoreCase = true) == true ||
                                rawName?.contains("quarq", ignoreCase = true) == true ||
                                rawName?.contains("srm", ignoreCase = true) == true ||
                                rawName?.contains("favero", ignoreCase = true) == true
                        SensorType.HEART_RATE -> serviceUuids.contains(SingleSensorBleManager.HRM_SERVICE_UUID) ||
                                rawName?.contains("heart", ignoreCase = true) == true ||
                                rawName?.contains("hrm", ignoreCase = true) == true ||
                                rawName?.contains("polar", ignoreCase = true) == true ||
                                rawName?.contains("tickr", ignoreCase = true) == true ||
                                rawName?.contains("whoop", ignoreCase = true) == true ||
                                rawName?.contains("h10", ignoreCase = true) == true ||
                                rawName?.contains("h9", ignoreCase = true) == true
                    }

                    if (matchesType || (rawName != null && serviceUuids.isNotEmpty())) {
                        val displayName = rawName ?: "BLE ${type.title} ($address)"
                        val discovered = DiscoveredSensor(displayName, address, rssi, type)
                        _discoveredSensors.update { list ->
                            val existingIndex = list.indexOfFirst { it.address == address }
                            if (existingIndex >= 0) {
                                list.toMutableList().apply { set(existingIndex, discovered) }
                            } else {
                                list + discovered
                            }
                        }
                    }
                }

                override fun onScanFailed(errorCode: Int) {
                    _isScanning.value = false
                }
            }
            scanCallback = callback

            try {
                val settings = ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build()
                scanner.startScan(null, settings, callback)
            } catch (e: SecurityException) {
                _isScanning.value = false
            }
        }

        // Auto-stop scan after 15 seconds to save battery
        scanTimeoutJob = scope.launch {
            delay(15000L)
            stopScanning()
        }

        // If mock emulator mode is on and no real sensors detected after 2.5 seconds, inject simulated devices
        if (isMockEnabled) {
            scope.launch {
                delay(2500L)
                if (_discoveredSensors.value.isEmpty() && _isScanning.value) {
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
                }
            }
        }
    }

    fun stopScanning() {
        scanTimeoutJob?.cancel()
        scanTimeoutJob = null
        scanCallback?.let { callback ->
            try {
                if (hasScanPermission()) {
                    bluetoothAdapter?.bluetoothLeScanner?.stopScan(callback)
                }
            } catch (e: Exception) {
                // Ignore scanner stop exceptions
            }
            scanCallback = null
        }
        _isScanning.value = false
    }

    fun connectDevice(address: String, name: String, type: SensorType) {
        val adapter = bluetoothAdapter ?: return
        if (!hasConnectPermission()) return

        // If already connected, do nothing
        if (activeClients.containsKey(address)) return

        val device = try {
            adapter.getRemoteDevice(address)
        } catch (e: IllegalArgumentException) {
            // Address might be a simulated MAC on emulator
            when (type) {
                SensorType.CADENCE_SPEED -> _isCadenceConnected.value = true
                SensorType.POWER -> _isPowerConnected.value = true
                SensorType.HEART_RATE -> _isHeartRateConnected.value = true
            }
            return
        }

        val client = SingleSensorBleManager(
            context = context,
            sensorType = type,
            onCadenceUpdate = { rpm -> _cadenceFlow.value = rpm },
            onPowerUpdate = { watts -> _powerFlow.value = watts },
            onHeartRateUpdate = { hr -> _heartRateFlow.value = hr },
            onConnectionStateChange = { connected ->
                when (type) {
                    SensorType.CADENCE_SPEED -> _isCadenceConnected.value = connected
                    SensorType.POWER -> _isPowerConnected.value = connected
                    SensorType.HEART_RATE -> _isHeartRateConnected.value = connected
                }
            }
        )

        activeClients[address] = client
        client.connect(device)
            .useAutoConnect(true)
            .retry(3, 200)
            .enqueue()
    }

    fun disconnectDevice(address: String) {
        activeClients.remove(address)?.let { client ->
            client.disconnect().enqueue()
            when (client.sensorType) {
                SensorType.CADENCE_SPEED -> {
                    _isCadenceConnected.value = false
                    _cadenceFlow.value = 0
                }
                SensorType.POWER -> {
                    _isPowerConnected.value = false
                    _powerFlow.value = 0
                }
                SensorType.HEART_RATE -> {
                    _isHeartRateConnected.value = false
                    _heartRateFlow.value = 0
                }
            }
        }
    }

    fun disconnectAll() {
        stopScanning()
        activeClients.values.forEach { it.disconnect().enqueue() }
        activeClients.clear()
        _isCadenceConnected.value = false
        _isPowerConnected.value = false
        _isHeartRateConnected.value = false
        _cadenceFlow.value = 0
        _powerFlow.value = 0
        _heartRateFlow.value = 0
    }
}
