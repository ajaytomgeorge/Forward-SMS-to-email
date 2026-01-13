# SMS Forwarder 📱➡️📧

An Android app that automatically forwards incoming SMS messages to your email. Perfect for when you're traveling abroad and your home SIM (like Jio) doesn't work, but you still need to receive important SMS messages like OTPs and notifications.

## 🎯 Use Case

Created for travelers who:
- Are in a country where their home SIM provider doesn't work
- Need to receive SMS verification codes (OTPs) while abroad
- Want to stay connected to important SMS notifications without roaming

Just leave your phone with the home SIM at home (connected to WiFi), install this app, and all SMS messages will be forwarded to your email!

## 📥 Installation

### Option 1: Download APK (Recommended)
1. Download `app-debug.apk` from the [releases](./app/release/) folder
2. Transfer to your Android device
3. Enable "Install from unknown sources" if prompted
4. Install the APK

### Option 2: Build from Source
1. Clone this repository:
   ```bash
   git clone https://github.com/ajaytomgeorge/Forward-SMS-to-email.git
   ```
2. Open the project in Android Studio
3. Wait for Gradle sync to complete
4. Connect your Android device via USB (enable USB debugging)
5. Click **Run** (green play button) or use **Build → Build Bundle(s) / APK(s) → Build APK(s)**
6. The APK will be generated at: `app/build/outputs/apk/debug/app-debug.apk`

## 📱 Supported Android Versions

- **Minimum**: Android 7.0 (API 24)
- **Target**: Android 14 (API 36)
- **Tested on**: Samsung devices with Knox

## ⚙️ Setup Instructions

### 1. Grant Permissions
When you first open the app, grant the following permissions:
- **SMS** - Required to read incoming messages
- **Notifications** - Required for the foreground service

### 2. Configure Email Settings

| Field | Description | Example |
|-------|-------------|---------|
| **Target Email** | Email address to receive forwarded SMS | your.email@gmail.com |
| **SMTP Host** | Your email provider's SMTP server | smtp.gmail.com |
| **SMTP Port** | SMTP port (usually 587 for TLS) | 587 |
| **Username** | Your email address for sending | sender@gmail.com |
| **Password** | App-specific password (see below) | xxxx xxxx xxxx xxxx |

### 3. Gmail Setup (Recommended)

If using Gmail to send emails:

1. Go to [Google Account Security](https://myaccount.google.com/security)
2. Enable **2-Step Verification** if not already enabled
3. Go to **App passwords** (search for it in account settings)
4. Generate a new app password for "Mail"
5. Use this 16-character password in the app (not your regular Gmail password)

### 4. Start the Service
1. Fill in all email settings
2. Tap **Subscribe**
3. You should see a notification: "Monitoring for new SMS messages"
4. The service will now forward all incoming SMS to your email

## 🔧 Features

- ✅ Forwards SMS to email automatically
- ✅ Works in background with foreground service
- ✅ Auto-starts after phone reboot
- ✅ Handles multi-part (long) SMS messages
- ✅ Shows sender and timestamp in email
- ✅ Debug mode (long-press logs area)

## ⚠️ Important Notes

1. **SMS Only**: This app forwards traditional SMS messages only. RCS messages (chat features in Google Messages) are NOT supported.

2. **Battery Optimization**: For reliable background operation, disable battery optimization for this app:
   - Settings → Apps → SMS Forwarder → Battery → Unrestricted

3. **Keep WiFi Connected**: The phone needs internet access to send emails.

4. **Samsung/Knox Devices**: You may need to enable "Auto-start" permission in device settings.

## 🐛 Troubleshooting

**Messages not being forwarded?**
- Long-press the logs area to see debug info
- Check if "SMS Access" shows "OK"
- Verify you're receiving SMS (not RCS)
- Check email credentials are correct

**Service stops after a while?**
- Disable battery optimization for the app
- Enable auto-start permission
- Keep the app in the recent apps list

## 📄 License

MIT License - feel free to use and modify.

## 🙏 Acknowledgments

Built with ❤️ for travelers who need to stay connected to their home country's SMS while abroad.

🤖 Created with assistance from **Claude Opus** and **Gemini Pro** AI models.
