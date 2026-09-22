package com.velo.app.sensors

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer

class WearOsManager(private val context: Context) : MessageClient.OnMessageReceivedListener {

    companion object {
        private const val TAG = "WearOsManager"
        const val PATH_HEART_RATE = "/velo/heart_rate"
        const val PATH_RIDE_STATUS = "/velo/status"
        const val PATH_STATUS_REQUEST = "/velo/status_request"
        const val PATH_OPEN_COMPANION = "/velo/open_companion"
    }

    private val messageClient: MessageClient = Wearable.getMessageClient(context)
    private val nodeClient = Wearable.getNodeClient(context)

    private val _wearOsHeartRate = MutableStateFlow(0)
    val wearOsHeartRate: StateFlow<Int> = _wearOsHeartRate.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    @Volatile
    private var currentRideStatus: String = "IDLE"
    private var isListening = false

    fun startListening() {
        if (!isListening) {
            isListening = true
            messageClient.addListener(this)
            Log.d(TAG, "startListening: registered message listener")
        }
        nodeClient.connectedNodes.addOnSuccessListener { nodes ->
            Log.d(TAG, "startListening: connectedNodes count=${nodes.size}")
            if (nodes.isNotEmpty()) {
                _isConnected.value = true
            }
        }
    }

    fun stopListening() {
        if (isListening) {
            isListening = false
            messageClient.removeListener(this)
            Log.d(TAG, "stopListening: removed message listener")
        }
    }

    fun openCompanionApp() {
        val bytes = byteArrayOf(1)
        nodeClient.connectedNodes.addOnSuccessListener { nodes ->
            Log.d(TAG, "openCompanionApp: sending OPEN_COMPANION to ${nodes.size} nodes")
            for (node in nodes) {
                messageClient.sendMessage(node.id, PATH_OPEN_COMPANION, bytes)
            }
        }
    }

    fun sendRideStatus(status: String) {
        currentRideStatus = status
        val bytes = status.toByteArray(Charsets.UTF_8)
        nodeClient.connectedNodes.addOnSuccessListener { nodes ->
            Log.d(TAG, "sendRideStatus: sending status=$status to ${nodes.size} nodes")
            for (node in nodes) {
                messageClient.sendMessage(node.id, PATH_RIDE_STATUS, bytes)
            }
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        when (messageEvent.path) {
            PATH_HEART_RATE -> {
                _isConnected.value = true
                val data = messageEvent.data
                if (data.size >= 4) {
                    val hr = ByteBuffer.wrap(data).int
                    _wearOsHeartRate.value = hr
                    Log.d(TAG, "Received HR from watch: $hr bpm")
                }
            }
            PATH_STATUS_REQUEST -> {
                _isConnected.value = true
                val bytes = currentRideStatus.toByteArray(Charsets.UTF_8)
                messageClient.sendMessage(messageEvent.sourceNodeId, PATH_RIDE_STATUS, bytes)
                Log.d(TAG, "Received STATUS_REQUEST from ${messageEvent.sourceNodeId}, replied with $currentRideStatus")
            }
        }
    }
}
