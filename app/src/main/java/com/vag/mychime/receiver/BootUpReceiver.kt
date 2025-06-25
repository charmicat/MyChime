package com.vag.mychime.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

import com.vag.mychime.service.TimeService

class BootUpReceiver : BroadcastReceiver() {
    private val TAG = "BootUpReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive")

        if (Intent.ACTION_BOOT_COMPLETED == intent.action ||
            "RestartTimeService" == intent.action) {
            Log.d(TAG, "BootUpReceiver BOOT_COMPLETED")
            val i = Intent(context, TimeService::class.java)
            context.startService(i)
        }
    }
}
