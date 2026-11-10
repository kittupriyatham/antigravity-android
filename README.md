# Google Antigravity for Android 🚀

An Android application companion for **[Google Antigravity](https://antigravity.google.com)** (`antigravity.google.com`), enabling full mobile control over autonomous agents, live terminal tasks, artifact reviews, and command approval notifications.

---

## ⚡ Two Ways to Run on Android

### Option A: The Instant 10-Second Method (Official PWA / WebAPK)
Google Antigravity provides first-class **Progressive Web App (PWA)** support natively:
1. Open Chrome on your Android phone and navigate to `https://antigravity.google.com`.
2. Tap the three dots menu (**⋮**) in the top right corner.
3. Tap **"Install app"** (or **"Add to Home Screen"**).
4. Chrome creates an official standalone **WebAPK** on your phone:
   - Appears in your app drawer and home screen.
   - Runs in a borderless fullscreen window without browser URL bars.
   - Receives native Android push alerts when agents finish or request approvals.
   - Inherits your saved Google Account login instantly.

---

### Option B: Native Android Studio Project (This Codebase)
This repository contains a full **Kotlin + AndroidX** project configured for enterprise builds, custom branding, or sideloading:

* **Chrome Custom Tabs Launcher (`MainActivity.kt`)**: Recommended approach for 1-tap fast loading and 100% Google Single Sign-On compatibility.
* **Embedded In-App WebView (`WebViewActivity.kt`)**: Full-screen in-app container featuring:
  * **Google OAuth Bridge**: Automatically intercepts `accounts.google.com` to prevent `403: disallowed_useragent` errors.
  * **File Upload Support**: Full `WebChromeClient.onShowFileChooser` implementation for attaching workspace files and configs.
  * **Pull-to-Refresh**: Native `SwipeRefreshLayout`.
  * **Offline Error State**: Graceful retry screen when network drops.
  * **Push Notifications Ready**: Android 13+ `POST_NOTIFICATIONS` runtime permissions handled.

---

## 🛠️ How to Build & Run

### In Android Studio
1. Open Android Studio.
2. Select **Open an Existing Project** and choose the `antigravity-android` folder.
3. Wait for Gradle to sync dependencies.
4. Connect an Android device (or launch an emulator) and click **Run (Shift + F10)**.

### From Command Line
```bash
cd antigravity-android
./gradlew assembleDebug
```
The compiled APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 📱 Features

| Feature | Custom Tabs / TWA | Embedded WebView | PWA (WebAPK) |
| :--- | :---: | :---: | :---: |
| **Instant Setup** | Build Required | Build Required | ⚡ 10 Seconds |
| **Google Sign-In** | Seamless (Native) | Handled via OAuth Bridge | Seamless (Native) |
| **Push Notifications** | Supported | Supported | Supported |
| **File / Config Upload** | Supported | Supported | Supported |
| **Window Frame** | Clean Fullscreen | App Shell with Controls | Fullscreen Standalone |
