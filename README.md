# TestBrowser

A modern Android WebView browser focused on **Chrome fingerprint parity** with flexible configuration and a sane, testable networking stack.

---

## Highlights

- **UA Parity** — User-Agent derived from installed Chrome Stable (WebView fallback).  
- **Header Control** — Removes `X-Requested-With`; sets `Accept`, `Accept-Language`, `Cache-Control`, `Sec-Fetch-*`.  
- **Cronet + OkHttp** — Cronet as primary HTTP stack (HTTP/2, QUIC, Brotli); OkHttp fallback.  
- **Cookie Sync** — Full round-trip via `CookieManager`.  
- **Safe Decoding** — Handles `gzip/deflate/br` (and ignores `zstd` at runtime if native lib is absent).  
- **Redirects** — Manual handling (up to 10 hops).  
- **Config First** — Immutable `WebViewConfig` applied live with smart WebView recreation.

### What’s intentionally not spoofed
- `Sec-CH-UA*` are provided by Chromium/WebView and can expose WebView.
- TLS/HTTP2 low-level ordering (JA3/JA4, SETTINGS, GREASE) may differ slightly.
- WebView limitations: request body interception for some methods is not available.

---

## How it works

### Network proxy
- `shouldInterceptRequest` → **replay** via Cronet (or OkHttp) with Chrome-like headers.
- Forces `Accept-Encoding: identity` to avoid double decompression; then **optionally** decodes (gzip/deflate/br) and rewrites headers for WebView safety.
- Syncs cookies in both directions.
- Skips native/non-HTTP schemes (`blob:`, `data:`, `file:`, `intent:`, `ws:`, `wss:`).

### Configuration
- `WebViewConfig` is the single source of truth (DataStore).  
- Applying changes recreates the WebView **only when required** (diff-based).

### User agent
- `ChromeUAProvider` inspects installed Chrome; supports mobile/desktop and custom overrides.

---

## Settings

**Core:** Desktop mode, JavaScript, DOM Storage, Mixed Content, Force Dark Mode, JS Compatibility Layer, Third-party cookies.  
**Network:** Route traffic via **Chromium (Cronet)**, **Proxy Intercept Requests**, remove `X-Requested-With`, custom `Accept-Language`, custom UA.

---

## Quick test

1. Enable **Route traffic via Chromium (Cronet)** and **Proxy Intercept Requests**.  
2. Visit:
   - `https://tls.peet.ws/api/all` → check UA, `Accept-Language`, and **no** `X-Requested-With`.
   - `https://fingerprintjs.github.io/fingerprintjs/` → expect UA parity; CH hints may differ.
   - `https://httpbin.org/headers` → inspect effective headers.

---

## Architecture

<p align="center">
  <a href="https://github.com/user-attachments/assets/44da216e-9cde-4912-b4e0-26e4cbcc6440">
    <img src="https://github.com/user-attachments/assets/44da216e-9cde-4912-b4e0-26e4cbcc6440" alt="App architecture diagram" width="1100">
  </a>
</p>

---

## Screenshots

### Light mode
<table>
  <tr>
    <th>Start Page</th>
    <th>BrowserScan</th>
    <th>Advanced Settings</th>
  </tr>
  <tr>
    <td><img src="https://github.com/user-attachments/assets/47111eb4-6ac7-45ab-b8af-18bab93744c6" width="270" alt="Start Page (light)"></td>
    <td><img src="https://github.com/user-attachments/assets/ed20f2ac-b84e-4b16-98ed-25facbff05db" width="270" alt="BrowserScan (light)"></td>
    <td><img src="https://github.com/user-attachments/assets/03d9f351-e347-4045-8a20-7c3b56c6d6b3" width="270" alt="Advanced Settings (light)"></td>
  </tr>
</table>

### Dark mode
<table>
  <tr>
    <th>Start Page</th>
    <th>BrowserScan</th>
    <th>Advanced Settings</th>
  </tr>
  <tr>
    <td><img src="https://github.com/user-attachments/assets/b57fa9b1-c900-4cb8-a42d-ab782be0fedc" width="270" alt="Start Page (dark)"></td>
    <td><img src="https://github.com/user-attachments/assets/cefb7552-be74-4bd8-86b4-2b74faa62515" width="270" alt="BrowserScan (dark)"></td>
    <td><img src="https://github.com/user-attachments/assets/fac3462e-f8e9-46e4-a36d-a5bd14152095" width="270" alt="Advanced Settings (dark)"></td>
  </tr>
</table>

---

## Dependencies

- Cronet (Play Services or embedded)
- OkHttp **5.1.0**
- AndroidX WebKit, DataStore
- Kotlin Coroutines
- DI (Koin)
- Jetpack Compose (Material 3)

---

## Security

- Cleartext only allowed in development via `network_security_config`.
- SSL errors are blocked by default.
- Cookie security via `CookieManager`; file access is configurable.
