package com.example.smsforwarder

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

class EmailSender(private val context: Context) {

    suspend fun sendEmail(
        sender: String,
        messageBody: String,
        timestamp: Long,
        targetEmail: String,
        smtpHost: String,
        smtpPort: String,
        username: String,
        password: String
    ) = withContext(Dispatchers.IO) {
        try {
            log("Sending email...")
            val props = Properties()
            props["mail.smtp.auth"] = "true"
            props["mail.smtp.starttls.enable"] = "true"
            props["mail.smtp.host"] = smtpHost
            props["mail.smtp.port"] = smtpPort

            val session = Session.getInstance(props, object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(username, password)
                }
            })

            val message = MimeMessage(session)
            message.setFrom(InternetAddress(username))
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(targetEmail))
            message.subject = "New SMS from $sender"
            message.setText("From: $sender\nTime: ${Date(timestamp)}\n\n$messageBody")

            Transport.send(message)
            log("Email sent successfully")
        } catch (e: Exception) {
            log("Email failed: ${e.message}")
        }
    }

    private fun log(message: String) {
        val intent = Intent("log_update")
        intent.putExtra("log", message)
        context.sendBroadcast(intent)
    }
}
