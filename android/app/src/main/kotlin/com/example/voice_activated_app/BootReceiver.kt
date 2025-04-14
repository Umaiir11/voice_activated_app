package com.example.voice_activated_app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action.equals(Intent.ACTION_BOOT_COMPLETED, ignoreCase = true)) {
            Log.d("BootReceiver", "Device rebooted - starting VoiceService...")

            val serviceIntent = Intent(context, VoiceService::class.java)

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            } catch (e: Exception) {
                Log.e("BootReceiver", "Error starting VoiceService on boot", e)
            }
        }
    }
}
