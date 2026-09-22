package com.velo.wear.data

import androidx.compose.ui.graphics.Color
import com.velo.wear.theme.StatusConnected
import com.velo.wear.theme.StatusNotConnected
import com.velo.wear.theme.StatusPaused

enum class WatchRideStatus(val dotColor: Color) {
    CONNECTED(StatusConnected),       // Green
    PAUSED(StatusPaused),             // Yellow
    NOT_CONNECTED(StatusNotConnected); // Red

    companion object {
        fun fromRideStatusString(status: String?): WatchRideStatus {
            return when (status?.trim()?.uppercase()) {
                "PAUSED" -> PAUSED
                "NOT_CONNECTED", "DISCONNECTED", null, "" -> NOT_CONNECTED
                else -> CONNECTED
            }
        }
    }
}
