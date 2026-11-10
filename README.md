# Google Antigravity for Android 🚀

An Android application for **[Google Antigravity](https://antigravity.google.com)** (`antigravity.google.com`), providing an agent-first development experience directly on Android devices.

Featuring the **official Google Antigravity brand icon**, integrated workspace management, quick slash commands dock, file attachment support, and real-time push alerts.

---

## 📱 Features

- **Direct In-App Experience**: Opens straight into your Antigravity agent workspace with zero launcher menus.
- **Official Brand Icon**: High-definition adaptive vector and mipmap icons using Google Antigravity's official multi-color gradient arch.
- **Instance Switcher**: Seamlessly switch between Google Cloud Remote Agents, desktop workstations running the `agy` daemon, or custom hosts.
- **Quick Slash Commands Dock**: One-tap injection of `/diff`, `/goal`, `/plan`, `/teamwork`, and `/grill-me`.
- **File & Config Uploads**: Full `WebChromeClient.onShowFileChooser` integration with Android's system file picker for attaching project files, configs, and screenshots.
- **Voice Mode & Audio**: Audio recording permissions enabled for Gemini Live voice mode in Antigravity.
- **Push Alerts**: Android 13+ runtime notifications for agent approvals and completions.
- **Google OAuth Bridge**: Chrome Custom Tabs authentication fallback ensuring secure sign-in without `disallowed_useragent` blocks.

---

## 🛠️ Build & Release

```bash
cd antigravity-android
./gradlew assembleDebug
```
The compiled APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`
