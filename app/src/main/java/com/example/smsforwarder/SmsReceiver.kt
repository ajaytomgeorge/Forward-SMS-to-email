package com.example.smsforwarder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("SmsReceiver", "onReceive called with action: ${intent.action}")
        
        // Always log to SharedPreferences for debugging
        val prefs = context.getSharedPreferences("com.example.smsforwarder.PREFS", Context.MODE_PRIVATE)
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        prefs.edit().putString("last_receiver_call", "onReceive at $timestamp").apply()
        
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            Log.d("SmsReceiver", "SMS_RECEIVED_ACTION detected!")
            sendLog(context, "→ MESSAGE_DETECTED")
            prefs.edit().putString("last_sms_detected", "SMS at $timestamp").apply()

            // Check if service is running
            if (!prefs.getBoolean("is_running", false)) {
                Log.d("SmsReceiver", "Service not running, ignoring SMS")
                sendLog(context, "❌ Service not running, ignoring")
                return
            }

            sendLog(context, "→ EXTRACTING_MESSAGE")

            // Extract SMS messages
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            if (messages.isNullOrEmpty()) {
                Log.d("SmsReceiver", "No messages found in intent")
                sendLog(context, "❌ No messages found")
                return
            }

            // Combine message parts (for long SMS that span multiple parts)
            val sender = messages[0].displayOriginatingAddress ?: "Unknown"
            val messageBody = messages.joinToString("") { it.messageBody ?: "" }
            val smsTimestamp = messages[0].timestampMillis

            Log.d("SmsReceiver", "SMS from: $sender")
            sendLog(context, "   From: $sender")
            sendLog(context, "   Body: ${messageBody.take(50)}${if (messageBody.length > 50) "..." else ""}")

            sendLog(context, "→ FORWARDING_MAIL")

            // Enqueue EmailWorker to send the email
            val inputData = Data.Builder()
                .putString("sender", sender)
                .putString("message_body", messageBody)
                .putLong("timestamp", smsTimestamp)
                .build()

            val emailWorkRequest = OneTimeWorkRequestBuilder<EmailWorker>()
                .setInputData(inputData)
                .build()

            WorkManager.getInstance(context).enqueue(emailWorkRequest)
            sendLog(context, "   Email work enqueued...")
            prefs.edit().putString("last_email_enqueued", "Enqueued at $timestamp for $sender").apply()
        }
    }

    private fun sendLog(context: Context, message: String) {
        Log.d("SmsReceiver", "Sending log: $message")
        val logIntent = Intent("log_update")
        logIntent.putExtra("log", message)
        context.sendBroadcast(logIntent)
    }
}
