# Google Antigravity for Android 🚀

A **100% Native Android Application** for **[Google Antigravity](https://antigravity.google.com)** (`antigravity.google.com`), providing an authentic, agent-first development experience directly on Android devices with **zero WebViews**.

Featuring the **official Google Antigravity brand icon**, Antigravity SDK integration, native Material 3 touch controls, native command approval cards, syntax-highlighted diffs, and live terminal console.

---

## 📱 100% Native Features (Zero WebView)

- **Pure Android Native UI**:
  - `RecyclerView` chat stream with native user & agent speech bubbles.
  - Collapsible **Thinking Process** accordions displaying real-time reasoning steps.
  - Native **Tool Approval Cards** with tactile, ripple-enabled **[Approve & Run]** and **[Reject]** buttons.
  - Syntax-highlighted **Diff Blocks** displaying file additions and deletions.
- **Antigravity SDK & Backend Integration**:
  - Communicates directly with Antigravity endpoints (Gemini API Antigravity Agent & local `agy` runtime daemon via OkHttp & JSON-RPC).
  - Native model selector (`Gemini 2.5 Pro`, `Gemini 2.5 Flash`, `Claude 3.5 Sonnet`, `GPT-4o`).
- **Official Brand Icon**:
  - High-definition adaptive vector and mipmap icons using Google Antigravity's official multi-color gradient arch.
- **Quick Slash Commands Dock**:
  - Native horizontal chips for `/diff`, `/goal`, `/plan`, `/teamwork`, and `/grill-me`.
- **Live Terminal & Console**:
  - Native interactive bash terminal with scrollable monospace output buffer and direct command executor.
- **File & Config Uploads**:
  - Android system file picker integration (`ActivityResultContracts.GetContent()`) for attaching workspace files, configs, and screenshots.

---

## 📦 Download APKs

Always available from the GitHub Releases:

* **Latest Release (`latest` tag)**:
  * **[Google-Antigravity-v1.3.0.apk](https://github.com/kittupriyatham/antigravity-android/releases/download/latest/Google-Antigravity-v1.3.0.apk)**
  * **[Antigravity-v1.3.0.apk](https://github.com/kittupriyatham/antigravity-android/releases/download/latest/Antigravity-v1.3.0.apk)**
* **Versioned Release (`v1.3.0` tag)**:
  * **[Google-Antigravity-v1.3.0.apk](https://github.com/kittupriyatham/antigravity-android/releases/download/v1.3.0/Google-Antigravity-v1.3.0.apk)**
  * **[Antigravity-v1.3.0.apk](https://github.com/kittupriyatham/antigravity-android/releases/download/v1.3.0/Antigravity-v1.3.0.apk)**

---

## 🛠️ Build from Source

```bash
cd antigravity-android
./gradlew assembleDebug
```
The compiled APK will be generated at:
`app/build/outputs/apk/debug/Google-Antigravity-v1.3.0.apk`
