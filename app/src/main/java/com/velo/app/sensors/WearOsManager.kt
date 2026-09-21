package com.velo.app.sensors

import android.content.Context
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer

class WearOsManager(private val context: Context) : MessageClient.OnMessageReceivedListener {

    companion object {
        const val PATH_HEART_RATE = "/velo/heart_rate"
    }

    private val messageClient: MessageClient = Wearable.getMessageClient(context)

    private val _wearOsHeartRate = MutableStateFlow(0)
    val wearOsHeartRate: StateFlow<Int> = _wearOsHeartRate.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    fun startListening() {
        messageClient.addListener(this)
    }

    fun stopListening() {
        messageClient.removeListener(this)
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == PATH_HEART_RATE) {
            _isConnected.value = true
            val data = messageEvent.data
            if (data.size >= 4) {
                val hr = ByteBuffer.wrap(data).int
                _wearOsHeartRate.value = hr
            }
        }
    }
}
