package com.velo.wear

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.velo.wear.communication.WearCopilotManager
import com.velo.wear.communication.WearOsDataListenerService
import com.velo.wear.theme.VeloWearTheme
import com.velo.wear.ui.CopilotScreen

class MainActivity : ComponentActivity() {

    private lateinit var copilotManager: WearCopilotManager

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        copilotManager.registerHeartRateSensor()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Cancel any pending launch notifications
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.cancel(WearOsDataListenerService.NOTIFICATION_ID)

        copilotManager = WearCopilotManager(this)

        checkAndRequestPermissions()

        setContent {
            val status by copilotManager.status.collectAsStateWithLifecycle()
            VeloWearTheme {
                CopilotScreen(status = status)
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf(Manifest.permission.BODY_SENSORS)
        if (Build.VERSION.SDK_INT >= 34) {
            permissions.add("android.permission.health.READ_HEART_RATE")
        }
        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    override fun onStart() {
        super.onStart()
        copilotManager.start()
    }

    override fun onResume() {
        super.onResume()
        checkAndRequestPermissions()
        copilotManager.registerHeartRateSensor()
    }

    override fun onStop() {
        super.onStop()
        copilotManager.stop()
    }
}
