package com.example.smsforwarder

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var targetEmailInput: com.google.android.material.textfield.TextInputEditText
    private lateinit var smtpHostInput: com.google.android.material.textfield.TextInputEditText
    private lateinit var smtpPortInput: com.google.android.material.textfield.TextInputEditText
    private lateinit var usernameInput: com.google.android.material.textfield.TextInputEditText
    private lateinit var passwordInput: com.google.android.material.textfield.TextInputEditText
    private lateinit var actionButton: Button
    private lateinit var logsText: TextView

    private lateinit var prefs: SharedPreferences

    private val logReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val log = intent.getStringExtra("log")
            logsText.append("\n$log")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences("com.example.smsforwarder.PREFS", MODE_PRIVATE)

        statusText = findViewById(R.id.statusText)
        targetEmailInput = findViewById(R.id.targetEmailInput)
        smtpHostInput = findViewById(R.id.smtpHostInput)
        smtpPortInput = findViewById(R.id.smtpPortInput)
        usernameInput = findViewById(R.id.usernameInput)
        passwordInput = findViewById(R.id.passwordInput)
        actionButton = findViewById(R.id.actionButton)
        logsText = findViewById(R.id.logsText)
        logsText.text = "Logs: Waiting for message..."

        loadSettings()
        updateUI()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(logReceiver, IntentFilter("log_update"), RECEIVER_EXPORTED)
        } else {
            registerReceiver(logReceiver, IntentFilter("log_update"))
        }

        actionButton.setOnClickListener {
            if (isRunning()) {
                stopService()
            } else {
                startService()
            }
        }

        checkPermissions()
        
        // Long-press on logs to show debug info
        logsText.setOnLongClickListener {
            showDebugInfo()
            true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(logReceiver)
    }

    private fun checkPermissions() {
        val permissionsToRequest = mutableListOf<String>()
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.RECEIVE_SMS)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.READ_SMS)
        }
        
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), 101)
        }
    }

    private fun loadSettings() {
        targetEmailInput.setText(prefs.getString("target_email", ""))
        smtpHostInput.setText(prefs.getString("smtp_host", "smtp.gmail.com"))
        smtpPortInput.setText(prefs.getString("smtp_port", "587"))
        usernameInput.setText(prefs.getString("username", ""))
        passwordInput.setText(prefs.getString("password", ""))
    }

    private fun saveSettings() {
        prefs.edit()
            .putString("target_email", targetEmailInput.text.toString())
            .putString("smtp_host", smtpHostInput.text.toString())
            .putString("smtp_port", smtpPortInput.text.toString())
            .putString("username", usernameInput.text.toString())
            .putString("password", passwordInput.text.toString())
            .apply()
    }

    private fun isRunning(): Boolean {
        return prefs.getBoolean("is_running", false)
    }

    private fun startService() {
        saveSettings()
        
        // Validate credentials
        val targetEmail = targetEmailInput.text.toString()
        val username = usernameInput.text.toString()
        val password = passwordInput.text.toString()
        
        if (targetEmail.isEmpty() || username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all email settings", Toast.LENGTH_LONG).show()
            logsText.text = "Logs: ❌ Credentials not configured"
            return
        }
        
        prefs.edit().putBoolean("is_running", true).apply()
        updateUI()
        logsText.text = "Logs: ✅ Credentials validated\n→ Starting SMS observer service..."
        
        // Start the foreground service
        val serviceIntent = Intent(this, SmsObserverService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        
        Toast.makeText(this, "Service Started", Toast.LENGTH_SHORT).show()
    }
    
    private fun showDebugInfo() {
        val lastReceiverCall = prefs.getString("last_receiver_call", "Never")
        val lastSmsDetected = prefs.getString("last_sms_detected", "Never")
        val lastEmailEnqueued = prefs.getString("last_email_enqueued", "Never")
        val isRunning = prefs.getBoolean("is_running", false)
        val serviceStatus = prefs.getString("service_status", "Never started")
        val smsAccessTest = prefs.getString("sms_access_test", "Not tested")
        val observerStatus = prefs.getString("observer_status", "Not registered")
        val lastObserverTrigger = prefs.getString("last_observer_trigger", "Never")
        val lastError = prefs.getString("last_error", "None")
        
        logsText.text = """
            |=== DEBUG INFO ===
            |App Running: $isRunning
            |Service: $serviceStatus
            |SMS Access: $smsAccessTest
            |Observer: $observerStatus
            |Last Trigger: $lastObserverTrigger
            |Last SMS: $lastSmsDetected
            |Last Email: $lastEmailEnqueued
            |Last Error: $lastError
            |==================
            |Long-press to refresh
        """.trimMargin()
    }

    private fun stopService() {
        prefs.edit().putBoolean("is_running", false).apply()
        
        // Stop the foreground service
        val serviceIntent = Intent(this, SmsObserverService::class.java)
        stopService(serviceIntent)
        
        updateUI()
        logsText.text = "Logs: Service stopped"
        Toast.makeText(this, "Service Stopped", Toast.LENGTH_SHORT).show()
    }

    private fun updateUI() {
        val isRunning = isRunning()

        targetEmailInput.isEnabled = !isRunning
        smtpHostInput.isEnabled = !isRunning
        smtpPortInput.isEnabled = !isRunning
        usernameInput.isEnabled = !isRunning
        passwordInput.isEnabled = !isRunning

        if (isRunning) {
            statusText.text = getString(R.string.status_running)
            statusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
            actionButton.text = getString(R.string.unsubscribe_btn)
        } else {
            statusText.text = getString(R.string.status_stopped)
            statusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
            actionButton.text = getString(R.string.subscribe_btn)
        }
    }
}
