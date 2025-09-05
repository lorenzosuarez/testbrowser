# TestBrowser – Chrome Parity Fingerprint Browser

![Kotlin](https://img.shields.io/badge/Kotlin-1.9%2B-blue?logo=kotlin)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-UI-green?logo=jetpackcompose)
![Min SDK](https://img.shields.io/badge/minSdk-26-informational)
![License](https://img.shields.io/badge/License-Apache--2.0-yellow)
![Build](https://img.shields.io/badge/CI-Gradle-lightgrey)

---

## Overview
TestBrowser is a single-tab Android WebView browser built with Kotlin and Jetpack Compose.  
Its goal is to approximate Chrome Mobile behavior for fingerprinting research, HTTP header inspection, and controlled Web API availability testing.  

The app focuses on Chrome-like parity rather than full browser UX. It is lightweight, configurable, and ideal for reproducible experiments.

---

## Key Features
- **Chrome-like User Agent** with mobile/desktop/custom options.  
- **Navigation controls**: back, forward, reload, address bar.  
- **File uploads** via system picker.  
- **File downloads** including `blob:` URLs with JS bridge support.  
- **Persistent storage** (cookies, Web Storage, IndexedDB) with DataStore-backed configuration.  
- **Advanced configuration dialog** for runtime tuning.  
- **Diagnostics snapshot**: UA, locale, DPR, WebGL, storage.  
- **Unified Top/Bottom bar colors** using Material 3.  
- **Optional ad-blocking & privacy filters**.  

---

## Advanced Configuration
Accessible from the top app bar menu (⋮ → Advanced Config).

- **User Agent**: Chrome parity, mobile/desktop switch, custom UA, Accept-Language override.  
- **Web APIs**: JS, DOM Storage, Mixed Content, Media autoplay.  
- **Layout**: Dark mode, text zoom, viewport options.  
- **Storage**: Clear cookies and storage, reset runtime flags.  
- **Files & Downloads**: Standard + blob bridging, auto-open option.  
- **Diagnostics**: Snapshot with UA, DPR, WebGL, storage.  
- **Persistence**: Save last URL and config, theme mode.  
- **Privacy**: Optional ad-blocking and CSP injection.  

---

## Architecture
- Clean Architecture + SOLID principles.  
- **MVI/UDF**: immutable state, sealed intents, pure reducer.  
- **Layers**: UI (Compose), Domain, Data, Platform bridges.  
- **Koin DI** for modular dependency injection.  
- **Coroutines/Flows** for reactive state management.  

---

## Tech Stack
- Kotlin + Coroutines / Flow  
- Jetpack Compose (Material 3, dynamic color)  
- Android WebView / WebKit APIs  
- Koin (DI)  
- DataStore Proto (persistence)  
- Android DownloadManager + SAF  
- Coil (optional favicon rendering)  

---

## Getting Started
**Requirements:** Android Studio Giraffe+, JDK 17, device or emulator API 26+.  

**Build & Run:**  
1. Clone the repository.  
2. Sync Gradle in Android Studio.  
3. Build & Run.  
4. First launch opens a blank page.  

**Open Advanced Config:**  
- Tap the top app bar (⋮) → Advanced Config.  

---

## Testing Fingerprint Parity
1. Navigate to [httpbin.org/headers](https://httpbin.org/headers)  
   - Verify: UA matches Chrome stable format.  
   - Verify: `X-Requested-With` header is absent.  
2. Navigate to [browserscan.net](https://www.browserscan.net)  
   - Compare results with Chrome on the same device.  
   - Note: `Sec-CH-UA*` client hints may differ (engine controlled).  

Expected acceptable differences:  
- Client Hints brand lists may include `"Not A Brand"` or `"Chromium"`.  
- Minor variations in high-entropy fingerprinting APIs are expected.  

---

## Downloads & Uploads
- **Uploads**: handled via `onShowFileChooser` and system picker.  
- **Downloads**:  
  - Standard HTTP(S): delegated to DownloadManager.  
  - `blob:` URLs: supported with JS bridge + streaming.  

---

## Configuration & Theming
- Unified Top/Bottom bar colors via Material 3.  
- Dark/Light/System theme modes.  
- Dynamic color support when available.  

---

## Quality Gates & Tests
- Unit tests for UA provider, reducer, and config serialization.  
- Koin `checkModules()` for DI graph validation.  
- Instrumented smoke tests: navigation, uploads, downloads.  
- Static analysis with detekt and ktlint.  

---

## Performance & Security
- Single WebView instance reused across sessions.  
- StrictMode enabled in debug builds.  
- Minimal permissions; cleartext traffic disabled except for test hosts.  
- JS interfaces scoped and namespaced.  

---

## Limitations
- Client Hints (`Sec-CH-UA*`) cannot be fully overridden.  
- Some Web APIs may differ from standalone Chrome.  
- Not a full browser: no multi-tab or history UI.  

---

## Screenshots (Placeholders)

| TestBrowser | Chrome |
|------------------|------------------------|
| ![](https://github.com/user-attachments/assets/b2707cd3-2f32-44c8-9750-e7a565e3bce8) | ![](https://github.com/user-attachments/assets/b580f8b0-6541-437e-a20d-b48550a67504) |

| Main UI | Advanced Config Dialog |
|---------|------------------------|
| ![](https://github.com/user-attachments/assets/f1e02cfc-197f-4b80-9147-4a6a315dd99d) | ![](https://github.com/user-attachments/assets/8f3573d9-04b6-4728-b97d-0d1448ac1364) |

