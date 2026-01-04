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
import com.camille.steply.pages.Home
import com.camille.steply.data.MidnightBaselineWorker
import com.camille.steply.ui.theme.SteplyTheme

class MainActivity : ComponentActivity() {

    // ---------- PERMISSION LAUNCHER ----------
    private val activityPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            // Se granted = true -> ok
            // Se false -> i passi resteranno 0 finché non concede il permesso
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ---------- SCHEDULA RESET A MEZZANOTTE ----------
        MidnightBaselineWorker.scheduleNext(this)

        // ---------- RICHIESTA PERMESSO (Android 10+) ----------
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val permission = Manifest.permission.ACTIVITY_RECOGNITION
            if (ContextCompat.checkSelfPermission(this, permission)
                != PackageManager.PERMISSION_GRANTED
            ) {
                activityPermissionLauncher.launch(permission)
            }
        }

        enableEdgeToEdge()

        setContent {
            SteplyTheme {
                Home()
            }
        }
    }
}
