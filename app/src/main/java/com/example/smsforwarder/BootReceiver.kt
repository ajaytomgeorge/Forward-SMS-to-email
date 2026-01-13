package com.example.smsforwarder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || 
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            
            Log.d("BootReceiver", "Boot completed, checking if service should start...")
            
            // Check if user had the service running before reboot
            val prefs = context.getSharedPreferences("com.example.smsforwarder.PREFS", Context.MODE_PRIVATE)
            val wasRunning = prefs.getBoolean("is_running", false)
            
            if (wasRunning) {
                Log.d("BootReceiver", "Service was running before reboot, starting SmsObserverService...")
                
                val serviceIntent = Intent(context, SmsObserverService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
                
                Log.d("BootReceiver", "SmsObserverService started after boot")
            } else {
                Log.d("BootReceiver", "Service was not running before reboot, skipping...")
            }
        }
    }
}
