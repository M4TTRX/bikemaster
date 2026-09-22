package com.velo.wear.communication

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.velo.wear.data.WatchRideStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.nio.ByteBuffer

class WearCopilotManager(private val context: Context) : MessageClient.OnMessageReceivedListener, SensorEventListener {

    companion object {
        private const val TAG = "WearCopilotManager"
        const val PATH_HEART_RATE = "/velo/heart_rate"
        const val PATH_RIDE_STATUS = "/velo/status"
        const val PATH_STATUS_REQUEST = "/velo/status_request"
    }

    private val messageClient: MessageClient = Wearable.getMessageClient(context)
    private val nodeClient = Wearable.getNodeClient(context)
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val hrSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_HEART_RATE)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _status = MutableStateFlow(WatchRideStatus.NOT_CONNECTED)
    val status: StateFlow<WatchRideStatus> = _status.asStateFlow()

    private var isListening = false
    private var isSensorRegistered = false

    fun start() {
        if (isListening) return
        isListening = true

        messageClient.addListener(this)
        registerHeartRateSensor()

        // Periodically verify connection and request status
        scope.launch {
            while (isActive) {
                checkConnectionAndRequestStatus()
                delay(3000L)
            }
        }
    }

    fun registerHeartRateSensor() {
        if (isSensorRegistered) return
        hrSensor?.let { sensor ->
            val registered = sensorManager?.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
            isSensorRegistered = (registered == true)
            Log.d(TAG, "Registered HR sensor listener: $registered")
        }
    }

    fun stop() {
        if (!isListening) return
        isListening = false

        messageClient.removeListener(this)
        sensorManager?.unregisterListener(this)
        isSensorRegistered = false
        scope.cancel()
    }

    private fun checkConnectionAndRequestStatus() {
        nodeClient.connectedNodes.addOnSuccessListener { nodes ->
            if (nodes.isEmpty()) {
                _status.value = WatchRideStatus.NOT_CONNECTED
            } else {
                if (_status.value == WatchRideStatus.NOT_CONNECTED) {
                    _status.value = WatchRideStatus.CONNECTED
                }
                // Connected to phone, request current ride status
                val bytes = byteArrayOf(1)
                for (node in nodes) {
                    messageClient.sendMessage(node.id, PATH_STATUS_REQUEST, bytes)
                }
            }
        }.addOnFailureListener {
            _status.value = WatchRideStatus.NOT_CONNECTED
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == PATH_RIDE_STATUS) {
            val statusString = String(messageEvent.data, Charsets.UTF_8)
            val newStatus = WatchRideStatus.fromRideStatusString(statusString)
            _status.value = newStatus
            Log.d(TAG, "Received ride status: $statusString -> $newStatus")
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_HEART_RATE && event.values.isNotEmpty()) {
            val bpm = event.values[0].toInt()
            Log.d(TAG, "SensorManager HR event: $bpm bpm (accuracy=${event.accuracy})")
            if (bpm > 0) {
                broadcastHeartRate(bpm)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun broadcastHeartRate(bpm: Int) {
        val bytes = ByteBuffer.allocate(4).putInt(bpm).array()
        nodeClient.connectedNodes.addOnSuccessListener { nodes ->
            Log.d(TAG, "Broadcasting HR $bpm to ${nodes.size} nodes")
            for (node in nodes) {
                messageClient.sendMessage(node.id, PATH_HEART_RATE, bytes)
            }
        }
    }
}
