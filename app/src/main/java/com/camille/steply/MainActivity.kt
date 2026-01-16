package com.camille.steply

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.camille.steply.data.MidnightBaselineWorker
import com.camille.steply.pages.MainNavGraph
import com.camille.steply.ui.theme.SteplyTheme

class MainActivity : ComponentActivity() {

    // Launcher for ACTIVITY_RECOGNITION
    private val activityRecognitionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
            // After ACTIVITY_RECOGNITION result, request notifications (if needed)
            requestPostNotificationsIfNeeded()
        }

    // Launcher for POST_NOTIFICATIONS
    private val postNotificationsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
            // Done. You can react to the result if needed.
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Schedule reset at midnight
        MidnightBaselineWorker.scheduleNext(this)

        enableEdgeToEdge()

        // Request permissions in a controlled sequence
        requestActivityRecognitionThenNotifications()

        setContent {
            SteplyTheme {
                MainNavGraph()
            }
        }
    }

    private fun requestActivityRecognitionThenNotifications() {
        // 1) Ask ACTIVITY_RECOGNITION first (Android 10+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val p = Manifest.permission.ACTIVITY_RECOGNITION
            val granted = ContextCompat.checkSelfPermission(this, p) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                activityRecognitionLauncher.launch(p)
                return // IMPORTANT: wait for callback before continuing
            }
        }

        // If already granted (or not needed), go directly to notifications
        requestPostNotificationsIfNeeded()
    }

    private fun requestPostNotificationsIfNeeded() {
        // 2) Ask POST_NOTIFICATIONS (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val p = Manifest.permission.POST_NOTIFICATIONS
            val granted = ContextCompat.checkSelfPermission(this, p) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                postNotificationsLauncher.launch(p)
            }
        }
    }
}
