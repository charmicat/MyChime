package com.vag.mychime.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.vag.mychime.service.TimeService

class BootUpReceiver : BroadcastReceiver() {

    private val tag = "BootUpReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(tag, "onReceive")

        if (Intent.ACTION_BOOT_COMPLETED == intent.action || "RestartTimeService" == intent.action) {
            Log.d(tag, "BootUpReceiver BOOT_COMPLETED")
            context.startService(Intent(context, TimeService::class.java))
        }
    }
}
