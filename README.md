# TestBrowser

A modern Android WebView browser that strives for Chrome fingerprint parity while providing full web capabilities and configuration flexibility.

## Feature Highlights

### Chrome Parity
- User-Agent strings generated from the installed Chrome Stable version with WebView fallback
- Configurable `Accept-Language` header
- Removal of `X-Requested-With` header on all HTTP/HTTPS requests via Cronet proxy
- Standard headers (`Accept`, `Cache-Control`, `Sec-Fetch-*`) aligned with Chrome
- Cookie persistence compatible with Chrome
- Manual redirect handling up to 10 hops

### Known Limitations
- `Sec-CH-UA*` client hints originate from the Chromium engine and may expose WebView instead of Chrome
- TLS extension ordering (JA3/JA4) may differ slightly
- HTTP/2 SETTINGS and GREASE values can vary
- Request body proxying for POST/PUT is not available in `WebResourceRequest`

## Architecture

### Network Proxy (`NetworkProxy`)
- Intercepts HTTP/HTTPS traffic via `shouldInterceptRequest`
- Replays requests with Cronet (Brotli, Zstd, HTTP/2, QUIC) and falls back to OkHttp
- Removes `X-Requested-With`
- Synchronizes cookies using `CookieManager`
- Bypasses native schemes: `blob:`, `data:`, `file:`, `ws:`, `wss:`, `intent:`

### Configuration (`WebViewConfig`)
- Immutable configuration stored with DataStore
- Real-time application with smart WebView recreation
- Single source of truth for browser behavior

### User Agent (`ChromeUAProvider`)
- Detects installed Chrome version via `PackageManager`
- Falls back to WebView version
- Supports mobile/desktop switching and custom overrides

## Settings

### Core
- Desktop mode
- JavaScript enablement
- DOM storage
- Mixed content policy
- Force dark mode
- File access permissions
- Media autoplay

### Network
- Proxy toggle for header control
- `Accept-Language` customization
- Custom user agent string

### Apply & Restart
- Immediate configuration updates with WebView recreation
- Preserves cookies and storage
- Avoids unnecessary rebuilds with smart diffing

## Testing

### Sites
1. `https://tls.peet.ws/api/all` – verify absence of `X-Requested-With`, check user agent and `Accept-Language`
2. `https://fingerprintjs.github.io/fingerprintjs` – confirm `navigator.userAgent`, expect differences in `Sec-CH-UA*`
3. `https://httpbin.org/headers` – inspect headers, confirm `X-Requested-With` is absent

### Script
```bash
# Open TestBrowser
# Enable "Route traffic via proxy"
# Navigate to each test site and verify headers
# Toggle settings and use "Apply & Restart WebView" to observe changes
```

## Implementation Details

### WebView Recreation
Triggered by changes to:
- JavaScript
- DOM storage
- Mixed content
- File access
- Media autoplay
- Proxy
- Force dark mode

### Performance
- Singleton OkHttpClient with pooling
- Streaming response handling
- Efficient cookie sync
- Hardware acceleration

### Error Handling
- Fallback pages on proxy failure
- Detailed SSL error reporting
- HTTP status code handling
- 30 s network timeouts

## Security Considerations
- Network security config permits cleartext during development
- SSL errors rejected by default
- Configurable file access permissions
- Cookie security maintained via `CookieManager`

## Dependencies
- Cronet Embedded
- OkHttp 5.1.0
- AndroidX WebKit
- Kotlinx Coroutines
- Koin
- DataStore
- Jetpack Compose

## Development
The project follows clean architecture:
- **Domain** – business logic and entities
- **Data** – repositories with DataStore
- **Presentation** – MVI with `StateFlow`
- **UI** – Compose Material 3

## Screenshots

### Light mode
<table>
  <thead>
    <tr>
      <th>Start Page</th>
      <th>BrowserScan</th>
      <th>Advanced Settings</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td><img src="https://github.com/user-attachments/assets/47111eb4-6ac7-45ab-b8af-18bab93744c6" width="260" alt="Start Page (light)"></td>
      <td><img src="https://github.com/user-attachments/assets/ed20f2ac-b84e-4b16-98ed-25facbff05db" width="260" alt="BrowserScan (light)"></td>
      <td><img src="https://github.com/user-attachments/assets/03d9f351-e347-4045-8a20-7c3b56c6d6b3" width="260" alt="Advanced Settings (light)"></td>
    </tr>
  </tbody>
</table>

### Dark mode
<table>
  <thead>
    <tr>
      <th>Start Page</th>
      <th>BrowserScan</th>
      <th>Advanced Settings</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td><img src="https://github.com/user-attachments/assets/b57fa9b1-c900-4cb8-a42d-ab782be0fedc" width="260" alt="Start Page (dark)"></td>
      <td><img src="https://github.com/user-attachments/assets/cefb7552-be74-4bd8-86b4-2b74faa62515" width="260" alt="BrowserScan (dark)"></td>
      <td><img src="https://github.com/user-attachments/assets/fac3462e-f8e9-46e4-a36d-a5bd14152095" width="260" alt="Advanced Settings (dark)"></td>
    </tr>
  </tbody>
</table>

