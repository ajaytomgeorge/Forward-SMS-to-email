package com.example.smsforwarder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Telephony
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SmsObserverService : Service() {
    
    private var smsObserver: ContentObserver? = null
    private var lastProcessedId: Long = 0
    
    override fun onCreate() {
        super.onCreate()
        Log.d("SmsObserverService", "=== SERVICE CREATED ===")
        updateDebug("service_status", "Created at ${getTimestamp()}")
        sendLog("Service starting...")
        
        startForegroundNotification()
        
        // Try to read current SMS to verify access
        testSmsAccess()
        
        registerSmsObserver()
    }
    
    private fun getTimestamp(): String {
        return SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
    }
    
    private fun updateDebug(key: String, value: String) {
        getSharedPreferences("com.example.smsforwarder.PREFS", MODE_PRIVATE)
            .edit()
            .putString(key, value)
            .apply()
    }
    
    private fun testSmsAccess() {
        try {
            Log.d("SmsObserverService", "Testing SMS access...")
            sendLog("Testing SMS access...")
            
            val cursor = contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                arrayOf(Telephony.Sms._ID, Telephony.Sms.ADDRESS, Telephony.Sms.DATE),
                null,
                null,
                "${Telephony.Sms.DATE} DESC LIMIT 1"
            )
            
            cursor?.use {
                if (it.moveToFirst()) {
                    val id = it.getLong(it.getColumnIndexOrThrow(Telephony.Sms._ID))
                    val addr = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS))
                    lastProcessedId = id
                    
                    updateDebug("last_sms_id", id.toString())
                    updateDebug("sms_access_test", "OK - Last SMS ID: $id from $addr at ${getTimestamp()}")
                    sendLog("✅ SMS access OK - monitoring from ID: $id")
                    Log.d("SmsObserverService", "SMS access OK, last ID: $id, from: $addr")
                } else {
                    updateDebug("sms_access_test", "OK but inbox empty at ${getTimestamp()}")
                    sendLog("✅ SMS access OK - inbox empty")
                    Log.d("SmsObserverService", "SMS access OK but inbox empty")
                }
            } ?: run {
                updateDebug("sms_access_test", "FAILED - cursor null at ${getTimestamp()}")
                sendLog("❌ SMS access FAILED - null cursor")
                Log.e("SmsObserverService", "SMS access FAILED - cursor is null")
            }
        } catch (e: SecurityException) {
            updateDebug("sms_access_test", "PERMISSION DENIED: ${e.message} at ${getTimestamp()}")
            sendLog("❌ SMS PERMISSION DENIED: ${e.message}")
            Log.e("SmsObserverService", "SMS Permission denied", e)
        } catch (e: Exception) {
            updateDebug("sms_access_test", "ERROR: ${e.message} at ${getTimestamp()}")
            sendLog("❌ SMS access error: ${e.message}")
            Log.e("SmsObserverService", "SMS access error", e)
        }
    }
    
    private fun startForegroundNotification() {
        val channelId = "sms_forwarder_channel"
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "SMS Forwarder Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
        
        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("SMS Forwarder")
            .setContentText("Monitoring for new SMS messages")
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        
        startForeground(1, notification)
        Log.d("SmsObserverService", "Foreground notification shown")
    }
    
    private fun registerSmsObserver() {
        Log.d("SmsObserverService", "Registering SMS observer...")
        sendLog("Registering SMS observer...")
        
        smsObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                Log.d("SmsObserverService", "=== CONTENT CHANGED (no uri) ===")
                updateDebug("last_observer_trigger", "onChange() at ${getTimestamp()}")
                sendLog("→ SMS_CONTENT_CHANGED")
                checkForNewSms()
            }
            
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)
                Log.d("SmsObserverService", "=== CONTENT CHANGED === URI: $uri")
                updateDebug("last_observer_trigger", "onChange(uri=$uri) at ${getTimestamp()}")
                sendLog("→ SMS_CONTENT_CHANGED (uri: $uri)")
                checkForNewSms()
            }
        }
        
        // Register for ALL SMS content changes
        contentResolver.registerContentObserver(
            Uri.parse("content://sms"),
            true,
            smsObserver!!
        )
        
        updateDebug("observer_status", "Registered at ${getTimestamp()}")
        sendLog("✅ Observer registered on content://sms")
        Log.d("SmsObserverService", "SMS observer registered successfully")
    }
    
    private fun checkForNewSms() {
        try {
            Log.d("SmsObserverService", "Checking for new SMS, lastProcessedId: $lastProcessedId")
            sendLog("Checking for new SMS...")
            
            val cursor = contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                arrayOf(
                    Telephony.Sms._ID,
                    Telephony.Sms.ADDRESS,
                    Telephony.Sms.BODY,
                    Telephony.Sms.DATE
                ),
                "${Telephony.Sms._ID} > ?",
                arrayOf(lastProcessedId.toString()),
                "${Telephony.Sms._ID} DESC"
            )
            
            cursor?.use {
                val count = it.count
                Log.d("SmsObserverService", "Found $count new messages")
                sendLog("Found $count new message(s)")
                
                if (it.moveToFirst()) {
                    val id = it.getLong(it.getColumnIndexOrThrow(Telephony.Sms._ID))
                    val sender = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)) ?: "Unknown"
                    val body = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.BODY)) ?: ""
                    val timestamp = it.getLong(it.getColumnIndexOrThrow(Telephony.Sms.DATE))
                    
                    Log.d("SmsObserverService", "New SMS: ID=$id, From=$sender")
                    sendLog("→ MESSAGE_DETECTED")
                    sendLog("   From: $sender")
                    sendLog("   Body: ${body.take(50)}${if (body.length > 50) "..." else ""}")
                    
                    // Update last processed ID
                    lastProcessedId = id
                    updateDebug("last_sms_id", id.toString())
                    updateDebug("last_sms_detected", "From $sender at ${getTimestamp()}")
                    
                    // Forward the SMS
                    forwardSms(sender, body, timestamp)
                }
            } ?: run {
                Log.e("SmsObserverService", "Query returned null cursor")
                sendLog("❌ Query failed - null cursor")
            }
        } catch (e: Exception) {
            Log.e("SmsObserverService", "Error checking SMS: ${e.message}", e)
            sendLog("❌ Error: ${e.message}")
            updateDebug("last_error", "${e.message} at ${getTimestamp()}")
        }
    }
    
    private fun forwardSms(sender: String, body: String, timestamp: Long) {
        sendLog("→ FORWARDING_MAIL")
        
        val inputData = Data.Builder()
            .putString("sender", sender)
            .putString("message_body", body)
            .putLong("timestamp", timestamp)
            .build()
        
        val emailWorkRequest = OneTimeWorkRequestBuilder<EmailWorker>()
            .setInputData(inputData)
            .build()
        
        WorkManager.getInstance(this).enqueue(emailWorkRequest)
        sendLog("   Email work enqueued...")
        
        updateDebug("last_email_enqueued", "For $sender at ${getTimestamp()}")
    }
    
    private fun sendLog(message: String) {
        Log.d("SmsObserverService", "LOG: $message")
        val logIntent = Intent("log_update")
        logIntent.putExtra("log", message)
        sendBroadcast(logIntent)
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("SmsObserverService", "=== SERVICE STARTED ===")
        updateDebug("service_status", "Started at ${getTimestamp()}")
        sendLog("→ Service running, waiting for SMS...")
        return START_STICKY
    }
    
    override fun onDestroy() {
        super.onDestroy()
        smsObserver?.let {
            contentResolver.unregisterContentObserver(it)
        }
        updateDebug("service_status", "Destroyed at ${getTimestamp()}")
        Log.d("SmsObserverService", "=== SERVICE DESTROYED ===")
        sendLog("Service stopped")
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
}
