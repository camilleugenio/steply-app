package com.camille.steply.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.camille.steply.data.StepDataStore
import com.camille.steply.service.StepForegroundService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return

        val pending = goAsync()

        CoroutineScope(Dispatchers.Default).launch {
            try {
                val store = StepDataStore(context.applicationContext)
                if (store.isTrackingEnabled()) {
                    StepForegroundService.start(context.applicationContext)
                }
            } finally {
                pending.finish()
            }
        }
    }
}