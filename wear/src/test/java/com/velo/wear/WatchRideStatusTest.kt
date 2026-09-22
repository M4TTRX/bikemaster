package com.velo.wear

import com.velo.wear.data.WatchRideStatus
import com.velo.wear.theme.StatusConnected
import com.velo.wear.theme.StatusNotConnected
import com.velo.wear.theme.StatusPaused
import org.junit.Assert.assertEquals
import org.junit.Test

class WatchRideStatusTest {

    @Test
    fun testStatusDotColors() {
        assertEquals(StatusConnected, WatchRideStatus.CONNECTED.dotColor)
        assertEquals(StatusPaused, WatchRideStatus.PAUSED.dotColor)
        assertEquals(StatusNotConnected, WatchRideStatus.NOT_CONNECTED.dotColor)
    }

    @Test
    fun testFromRideStatusString() {
        assertEquals(WatchRideStatus.CONNECTED, WatchRideStatus.fromRideStatusString("RECORDING"))
        assertEquals(WatchRideStatus.CONNECTED, WatchRideStatus.fromRideStatusString("recording"))
        assertEquals(WatchRideStatus.CONNECTED, WatchRideStatus.fromRideStatusString("CONNECTED"))
        assertEquals(WatchRideStatus.CONNECTED, WatchRideStatus.fromRideStatusString("IDLE"))

        assertEquals(WatchRideStatus.PAUSED, WatchRideStatus.fromRideStatusString("PAUSED"))
        assertEquals(WatchRideStatus.PAUSED, WatchRideStatus.fromRideStatusString("paused"))

        assertEquals(WatchRideStatus.NOT_CONNECTED, WatchRideStatus.fromRideStatusString("NOT_CONNECTED"))
        assertEquals(WatchRideStatus.NOT_CONNECTED, WatchRideStatus.fromRideStatusString("DISCONNECTED"))
        assertEquals(WatchRideStatus.NOT_CONNECTED, WatchRideStatus.fromRideStatusString(null))
        assertEquals(WatchRideStatus.NOT_CONNECTED, WatchRideStatus.fromRideStatusString(""))
    }
}
