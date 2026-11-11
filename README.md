# Google Antigravity for Android 🚀

A **100% Native Android Application** for **[Google Antigravity](https://antigravity.google.com)** (`antigravity.google.com`), providing an authentic, agent-first development experience directly on Android devices with **zero WebViews**.

Featuring the **official Google Antigravity brand icon**, Antigravity SDK integration, native Material 3 touch controls, native command approval cards, syntax-highlighted diffs, and a live terminal console.

---

## 📱 100% Native Features (Zero WebView)

- **Pure Android Native UI**:
  - `RecyclerView` chat stream with native user & agent speech bubbles.
  - Collapsible **Thinking Process** accordions displaying real-time reasoning steps.
  - Native **Tool Approval Cards** with tactile, ripple-enabled **[Approve & Run]** and **[Reject]** buttons.
  - Syntax-highlighted **Diff Blocks** displaying file additions and deletions.
- **Antigravity SDK & Architecture**:
  - **BYOK Gemini Direct**: Enter a Google AI Studio key (`AIzaSy...`) for direct, standalone cloud AI without consuming Antigravity subscription credits.
  - **Antigravity Workspace Daemon (`agy daemon`)**: Connect to a local or remote daemon bridge on port 8080 for real file edits, live git diffs, and workspace builds.
  - **Built-in Engineering Assistant**: Context-aware coding assistant answering Kotlin, Python, and architecture queries with zero unrequested tool approvals.
- **Next-Gen Antigravity Model Fleet**:
  - **Gemini 4 Argon** (Recommended default)
  - **Gemini 3.8 Flash**
  - **Claude Opus 5.5**
  - **Claude Sonnet 5.5**
  - **GPT-OSS 12B**
- **Official Brand Icon**:
  - High-definition adaptive vector and mipmap icons across all screen densities (`mdpi` to `xxxhdpi`) using Google Antigravity's multi-color gradient arch.
- **Quick Slash Commands Dock**:
  - Native horizontal chips for `/diff`, `/goal`, `/plan`, `/teamwork`, and `/grill-me`.
- **Live Terminal & Console**:
  - Native interactive bash terminal with scrollable monospace output buffer and direct command executor.
- **Persistent Signing Keystore**:
  - Standardized repository keystore (`app/debug.keystore`) ensures identical certificate fingerprints across all CI/local builds for seamless in-place updates via `adb install -r`.

---

## 📦 Download APKs

Always available from the GitHub Releases:

* **Latest Release (`latest` tag)**:
  * **[Google-Antigravity-v1.5.1.apk](https://github.com/kittupriyatham/antigravity-android/releases/download/latest/Google-Antigravity-v1.5.1.apk)** (Primary)
  * **[Antigravity-v1.5.1.apk](https://github.com/kittupriyatham/antigravity-android/releases/download/latest/Antigravity-v1.5.1.apk)** (Short name)
* **Versioned Release (`v1.5.1` tag)**:
  * **[Google-Antigravity-v1.5.1.apk](https://github.com/kittupriyatham/antigravity-android/releases/download/v1.5.1/Google-Antigravity-v1.5.1.apk)**
  * **[Antigravity-v1.5.1.apk](https://github.com/kittupriyatham/antigravity-android/releases/download/v1.5.1/Antigravity-v1.5.1.apk)**

---

## 🛠️ Build from Source

```bash
cd antigravity-android
./gradlew assembleDebug
```
The compiled APK will be generated at:
`app/build/outputs/apk/debug/Google-Antigravity-v1.5.1.apk`

---

## 💡 Future Roadmap & Resume Guide

When returning to this project in the future:
1. **Google Antigravity Cloud OAuth**: If Google releases an official public mobile OAuth2 client ID or mobile REST endpoint for `antigravity.google.com`, plug the OAuth authentication token directly into `AntigravitySdkClient.kt`.
2. **Dedicated Relay Server**: You can run a small WebSocket/REST daemon on a workstation or cloud VPS to pipe real terminal and filesystem events into the native Android UI.
