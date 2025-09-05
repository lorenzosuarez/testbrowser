# Test Browser: Chrome-Parity Fingerprint Harness

![Kotlin](https://img.shields.io/badge/Kotlin-1.9%2B-blue?logo=kotlin)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-UI-green?logo=jetpackcompose)
![Min SDK](https://img.shields.io/badge/minSdk-26-informational)
![License](https://img.shields.io/badge/License-Apache--2.0-yellow)
![Build](https://img.shields.io/badge/CI-Local__Gradle-lightgrey)

---

## Overview
A focused, single-tab Android WebView browser written in Kotlin + Jetpack Compose whose primary objective is to approximate Google Chrome Mobile (stable channel) behavior for browser fingerprinting research, reproducible network header inspection, and controlled feature parity experiments. The application intentionally keeps surface complexity low (one tab, no history UI chrome) to reduce confounders while maximizing control over User-Agent, feature exposure, storage, and diagnostic instrumentation.

Primary use cases:
- Validate how closely a tuned Android WebView can mimic Chrome Mobile for anti-fingerprinting experiments.
- Inspect HTTP request/response headers (e.g., User-Agent, absence of X-Requested-With) under controlled runtime options.
- Evaluate Web API availability and subtle client hint / UA-CH divergences.

Non-goals:
- Full multi-tab browsing UX.
- Feature breadth beyond what is needed for fingerprint, storage, and networking parity evaluation.

## Key Features
- Single WebView Tab: Simplifies lifecycle, caching, and memory analysis.
- Chrome-like UA Strategy: Customized User-Agent string alignment with Chrome stable channel (device-specific segment can be injected). Client Hints differences documented.
- Navigation Controls: Back, forward (if possible), reload, address bar with commit-on-go semantics, error state feedback.
- File Upload Support: Bridges `<input type="file">` via system picker, including camera capture intents where supported.
- Download Handling: Delegates to Android `DownloadManager`; supports `blob:` URL downloads via in-memory bridging + streams.
- Persistent Storage: Cookies, Web Storage, IndexedDB retained across launches unless explicitly cleared in Advanced Config.
- Diagnostics Overlay: Optional panel for: current UA, major WebKit / Chromium version, enabled features, storage usage hints.
- State Persistence: Remembers last URL, runtime configuration, theme mode, and advanced toggles via DataStore.
- Graceful Error Pages: Simple offline / SSL / navigation failure feedback layer.
- Runtime Configuration Dialog: In-app advanced sheet enabling live restarts of the WebView with new flags.

## Advanced Configuration
All runtime options are accessible from the overflow (⋮) menu: Advanced Config.

### 1. User Agent & Client Identity
- Base Chrome UA Emulation: Updates version components to mirror installed stable.
- Device Model Injection: Optionally toggle inclusion of build model tokens.
- Mobile/Desktop Switch: Force desktop UA variant (experimental; may trigger layout shifts).
- Accept-Language Override: Provide custom list (comma-separated) for locale sensitivity tests.
- Sec-CH-UA Hints (Informational): Display-only; cannot be fully overridden (WebView constraint).

### 2. Web APIs Exposure
- JavaScript Enable / Disable.
- DOM Storage Toggle (affects localStorage/sessionStorage).
- Mixed Content Mode (Default: never allow active mixed content; configurable for test hosts).
- Media Autoplay Policy: Allow muted-only vs full.
- Geolocation Prompt Simulation: Stub / pass-through (if implemented; otherwise disabled).
- Clipboard Access Gate: Optional JS interface wrapping.

### 3. Layout & Rendering
- Force Dark Mode (auto / force on / force off) using WebView dark strategy.
- Text Zoom Slider (80–130%).
- Wide Viewport & Overview Mode toggles.
- Scrollbar visibility (overlay vs inset) for visual artifact tests.

### 4. Storage Management
- Clear Cookies.
- Clear Web Storage (localStorage + IndexedDB + cache).
- Reset All Runtime Flags (factory baseline).
- Partition Experiment (planned): Simulate ephemeral session (in-memory only) for differential fingerprint capture.

### 5. Files / Downloads
- Default Download Directory Selector (within public downloads).
- Auto-Open Completed Downloads (off by default to avoid noisy UX).
- Blob Download Bridge (enable/disable for performance isolation).

### 6. Diagnostics & Instrumentation
- Verbose Console Logging (logged via Logcat + optional in-app tail view).
- WebChromeClient Event Traces: Progress, title changes, JS dialogs, console levels.
- Network Timing (coarse): Navigation start / first progress / finished timestamps.
- StrictMode Toggle: Enable thread & VM policies for performance debugging.

### 7. Persistence Behavior
- Persist Last URL (on/off).
- Remember Advanced Config (on/off) – when off, revert to baseline on each cold start.
- Theme Mode: System / Light / Dark (Material 3 dynamic colors when available).

### 8. Ad-Blocking & Privacy Filters (Advanced / Optional)
- Blocklist Source: Hosts file / EasyList subset (optional import; not bundled by default).
- Heuristics Layer: Simple substring and MIME-type filter for tracking pixels.
- CSP Injection (Experimental): Inject additional Content-Security-Policy meta for script restrictions on test pages (may break sites).
- Fingerprinting Surface Reductions (Optional): JS interface to mask high-entropy navigator fields (disabled by default to preserve parity goals).

## Architecture
- Paradigm: Unidirectional Data Flow (MVI/UDF) for browser state (URL, progress, config, errors).
- Clean Architecture Layers:
  - Presentation (Compose UI + ViewModels) – minimal logic.
  - Domain (use-cases for config mutation, storage clearing, UA generation).
  - Data (DataStore persistence, DownloadManager integration, blocklist repository).
  - Platform Bridges (WebView clients, file chooser handler, blob translator).
- SOLID: Narrow interfaces for storage & network instrumentation; Open/Closed for feature flags.
- DI: Koin modules separated by layer (appModule, dataModule, domainModule, platformModule).
- Side-Effects: Central effect handler emitting navigation, toasts, sheet open/close.
- Threading: Coroutines / Flows for reactive state + snapshot interoperability.

## Tech Stack
- Kotlin + Coroutines / Flow
- Jetpack Compose (Material 3, dynamic color)
- Android WebView / WebKit APIs
- Koin (dependency injection)
- DataStore (Preferences) for config persistence
- Android DownloadManager + SAF (file handling)
- Coil (optional favicon / image loading if present)
- Detekt / (optionally ktlint) for static analysis

## Getting Started
### Prerequisites
- Android Studio Giraffe or newer.
- JDK 17 (Gradle toolchain may auto-provision).
- Android device or emulator (API 26+; real device recommended for fingerprint realism).

### Build & Run
1. Clone repository.
2. Sync Gradle in Android Studio.
3. Build & Run (Debug variant).
4. First launch loads default start page (configurable).

### Open Advanced Configuration
- Tap overflow (⋮) in the top app bar.
- Select Advanced Config.
- Modify options; press Apply (may recreate the WebView if necessary).

## Testing Fingerprint Parity
1. Launch the app and navigate to https://httpbin.org/headers
   - Verify: User-Agent mirrors Chrome stable string format.
   - Verify: X-Requested-With header is absent.
2. Navigate to https://www.browserscan.net/ (or alternative such as https://device.ripe.net/ or https://browserleaks.com/)
   - Compare: navigator.userAgent matches Chrome format.
   - Observe Differences: navigator.userAgentData (brands & fullVersionList) likely diverge (WebView controlled, cannot fully align).
3. Record Accept-Language and platform exposures.
4. Optionally toggle Desktop UA and re-run to observe server adaptation.

Expected Acceptable Differences:
- Sec-CH-UA brand entries may include "Not A Brand" or omit certain Chrome trial entries.
- Some high-entropy APIs (font probing nuance, advanced canvas metrics) may differ due to engine vs standalone Chrome optimizations.
- WebRTC ICE candidate ordering may differ (if tested).

## Downloads & Uploads
- File Upload: Handled through onShowFileChooser, delegating to ACTION_OPEN_DOCUMENT with persistable URI permissions if needed.
- Camera / Capture: If input capture attributes encountered, intent fallback attempts (may require permissions).
- Standard HTTP Downloads: Enqueued into DownloadManager with notification visibility; completion broadcast triggers optional toast.
- blob: Downloads: Intercepted by injecting JavaScript to read Blob content, transmitting via a JS bridge, streaming to a temp file, then passed to DownloadManager (size threshold heuristic to prevent memory pressure).
- MIME Type Resolution: Uses response headers; fallback to URL guess and ContentResolver.

## Configuration & Theming
- Unified TopBar / BottomBar color harmonized via Material 3 dynamic scheme (if device supports Monet) else static palette.
- Status & Navigation Bar contrast adjusted dynamically.
- Dark Mode respect: Follows system by default; can be forced for rendering experiments.
- Address Bar: Debounced input; commit on IME action or Go button.

## Quality Gates & Tests
- Unit Tests: Cover UA generation, config serialization, and blob download segmentation.
- DI Verification: Koin check module executed in test instrumentation (ensures graph validity).
- Smoke Tests: Launch + load start page + toggle one config flag.
- Static Analysis: Detekt (and optionally ktlint) tasks enforce style & safety.
- Lint: Android Lint baseline updated periodically (no fatal issues allowed for release variant).

Suggested Commands:
```
./gradlew detekt
./gradlew test
./gradlew lintDebug
```

## Performance & Security
- Single WebView Instance: Minimizes memory churn and ensures cache coherence.
- StrictMode (configurable): Detects accidental disk/network on main thread during experiments.
- Permission Minimization: Only requested when initiating an operation needing it.
- Cleartext Traffic: Disabled globally except optional test host exemptions (if configured in Network Security Config).
- JS Interfaces: Namespaced & limited surface; no unvetted reflection exposure.
- Download Isolation: Uses app-scoped temp files before handing to DownloadManager to avoid partial artifacts.

## Limitations
- Client Hint (Sec-CH-UA*) values cannot be fully overridden; WebView mediates them independently of the User-Agent string.
- UA-CH brand lists and fullVersionList will differ from standalone Chrome.
- Some advanced media / codec negotiation paths may vary (Chrome proprietary optimizations absent).
- Not a full browser: no multi-tab, history UI, advanced permission management panels.

## Screenshots (Placeholders)
| UI | Advanced Config | BrowserScan |
|----|-----------------|------------|
| (screenshot_main.png) | (screenshot_config.png) | (screenshot_browserscan.png) |

_Add images to the repository (e.g., docs/images/) and update the paths above._

## License
Licensed under the Apache License, Version 2.0.

```
Apache License
Version 2.0, January 2004
http://www.apache.org/licenses/

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
```

---

### Attribution / Notes
Chrome is a trademark of Google LLC. This project uses Android WebView APIs and does not bundle or redistribute Chrome.
