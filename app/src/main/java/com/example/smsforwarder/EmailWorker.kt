package com.example.smsforwarder

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class EmailWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d("EmailWorker", "Starting email work")

            // Get SMS data from input
            val sender = inputData.getString("sender") ?: "Unknown"
            val messageBody = inputData.getString("message_body") ?: ""
            val timestamp = inputData.getLong("timestamp", System.currentTimeMillis())

            // Get email settings from SharedPreferences
            val prefs = context.getSharedPreferences("com.example.smsforwarder.PREFS", Context.MODE_PRIVATE)
            val targetEmail = prefs.getString("target_email", "") ?: ""
            val smtpHost = prefs.getString("smtp_host", "smtp.gmail.com") ?: "smtp.gmail.com"
            val smtpPort = prefs.getString("smtp_port", "587") ?: "587"
            val username = prefs.getString("username", "") ?: ""
            val password = prefs.getString("password", "") ?: ""

            // Validate settings
            if (targetEmail.isEmpty() || username.isEmpty() || password.isEmpty()) {
                Log.e("EmailWorker", "Email settings not configured")
                sendLog("❌ MAIL_FAILED: Settings not configured")
                return@withContext Result.failure()
            }

            sendLog("   Connecting to $smtpHost:$smtpPort")
            Log.d("EmailWorker", "Sending email to: $targetEmail")

            // Send the email
            val emailSender = EmailSender(context)
            emailSender.sendEmail(
                sender = sender,
                messageBody = messageBody,
                timestamp = timestamp,
                targetEmail = targetEmail,
                smtpHost = smtpHost,
                smtpPort = smtpPort,
                username = username,
                password = password
            )

            sendLog("✅ MAIL_SENT to $targetEmail")
            sendLog("→ Waiting for message...")
            Log.d("EmailWorker", "Email sent successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e("EmailWorker", "Failed to send email: ${e.message}", e)
            sendLog("❌ MAIL_FAILED: ${e.message}")
            sendLog("→ Waiting for message...")
            Result.failure()
        }
    }

    private fun sendLog(message: String) {
        val logIntent = Intent("log_update")
        logIntent.putExtra("log", message)
        context.sendBroadcast(logIntent)
    }
}
