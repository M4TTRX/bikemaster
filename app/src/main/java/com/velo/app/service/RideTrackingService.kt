package com.velo.app.service

import android.app.Service
import android.content.Intent
import android.os.IBinder

class RideTrackingService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
}
