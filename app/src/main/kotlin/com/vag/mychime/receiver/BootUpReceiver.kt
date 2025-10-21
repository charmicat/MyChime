package com.vag.mychime.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.vag.mychime.service.TimeService

class BootUpReceiver : BroadcastReceiver() {
    private val tag = "BootUpReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(tag, "onReceive")

        if (Intent.ACTION_BOOT_COMPLETED == intent.action || "RestartTimeService" == intent.action) {
            Log.d(tag, "BootUpReceiver BOOT_COMPLETED")

            val serviceIntent = Intent(context, TimeService::class.java)
            ContextCompat.startForegroundService(context, serviceIntent)
        }
    }
}
