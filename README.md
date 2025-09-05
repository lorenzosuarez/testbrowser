# TestBrowser - Advanced WebView with Fingerprint Parity

## Fingerprint Parity Features

This browser implements advanced network proxying and configuration management to achieve maximum fingerprint parity with Chrome while maintaining full WebView functionality.

### What is Matched (Chrome Parity)

✅ **User-Agent Strings**: Auto-generated Chrome-compatible UA strings using detected Chrome Stable version or WebView fallback

✅ **Accept-Language**: Configurable language preferences that match Chrome's format

✅ **X-Requested-With Removal**: Complete elimination of the `X-Requested-With` header on all HTTP/HTTPS requests through Cronet proxying

✅ **Standard Headers**: Proper `Accept`, `Cache-Control`, `Sec-Fetch-*` headers matching Chrome's behavior

✅ **Cookie Management**: Full cookie persistence and transmission compatible with Chrome

✅ **Redirect Handling**: Manual redirect following (301/302/303/307/308) up to 10 hops

### What Cannot Be Matched (Known Limitations)

❌ **Sec-CH-UA* Client Hints**: These are controlled by Chromium engine and cannot be fully overridden in WebView

❌ **Sec-CH-UA* Client Hints**: Controlled by Chromium engine and expose WebView/Chromium rather than Chrome

❌ **TLS Extension Ordering**: TLS fingerprints (JA3/JA4) depend on engine internals; Cronet improves parity but some differences remain

❌ **HTTP/2 SETTINGS/GREASE**: Frame ordering and GREASE values may vary from Chrome

❌ **Request Body Proxying**: WebResourceRequest API doesn't expose POST/PUT request bodies

## Architecture Overview

### Network Proxy Layer (`NetworkProxy`)
- Intercepts all HTTP/HTTPS requests via `shouldInterceptRequest`
- Recreates requests with Cronet when available (Brotli, Zstd, HTTP/2, QUIC) with OkHttp fallback
- Strips X-Requested-With header completely
- Maintains cookie consistency through CookieManager
- Bypasses native schemes: `blob:`, `data:`, `file:`, `ws:`, `wss:`, `intent:`

### Configuration Management (`WebViewConfig`)
- Immutable configuration with DataStore persistence
- Real-time settings application with smart WebView recreation
- Centralized source of truth for all browser behavior

### User Agent Provider (`ChromeUAProvider`)
- Detects Chrome Stable version via PackageManager
- Falls back to WebView version detection
- Supports desktop/mobile UA switching
- Custom UA override support

## Advanced Settings

### Core Browser Settings
- **Desktop Mode**: Switch between mobile/desktop user agent
- **JavaScript**: Enable/disable JavaScript execution
- **DOM Storage**: Control localStorage and sessionStorage
- **Mixed Content**: Allow/block mixed HTTP/HTTPS content
- **Force Dark Mode**: Apply dark theme to all pages
- **File Access**: Control file:// URL access
- **Media Autoplay**: Control autoplay behavior

### Network Settings
- **Proxy Toggle**: Enable/disable network request proxying (required for X-Requested-With removal)
- **Accept-Language**: Customize language preference header
- **Custom User Agent**: Override auto-generated UA string

### Apply & Restart WebView
- Instant configuration changes with controlled WebView recreation
- Preserves cookies and storage during restart
- Smart diffing to avoid unnecessary rebuilds

## Testing & Verification

### Required Test Sites

1. **tls.peet.ws/api/all** (Main frame test)
   - Verify X-Requested-With header is absent
   - Confirm UA matches Chrome Stable format
   - Check Accept-Language header

2. **fingerprintjs.github.io/fingerprintjs** (Fingerprinting test)
   - Verify navigator.userAgent matches generated UA
   - Note expected differences in Sec-CH-UA* hints

3. **httpbin.org/headers** (Comprehensive header test)
   - Verify User-Agent and Accept-Language headers
   - Confirm X-Requested-With absent on main frame and subresources
   - Use browser network tab to inspect all requests

### Test Script

```bash
# 1. Open TestBrowser app
# 2. Open Settings dialog (menu button)
# 3. Enable "Route traffic via proxy" (should be on by default)
# 4. Test each verification site:

# Main frame test
Navigate to: https://tls.peet.ws/api/all
Expected: X-Requested-With header absent, Chrome-compatible UA

# Fingerprinting test  
Navigate to: https://fingerprintjs.github.io/fingerprintjs
Expected: navigator.userAgent matches browser UA (some client hints differences expected)

# Header inspection
Navigate to: https://httpbin.org/headers
Expected: Proper User-Agent, Accept-Language, no X-Requested-With

# Settings verification
# 5. Toggle each setting in dialog
# 6. Use "Apply & Restart WebView" for immediate effect
# 7. Verify changes take effect without app restart
```

## Implementation Details

### WebView Recreation Logic
Smart recreation triggers on changes to:
- JavaScript enabled/disabled
- DOM storage enabled/disabled  
- Mixed content policy
- File access permissions
- Media autoplay settings
- Proxy enabled/disabled
- Force dark mode

### Performance Optimizations
- Singleton OkHttpClient with connection pooling
- Streaming response handling (no full buffering)
- Efficient cookie synchronization
- Hardware acceleration enabled

### Error Handling
- Graceful proxy failures with fallback error pages
- SSL error reporting with detailed messages
- HTTP error handling with proper status codes
- Network timeout management (30s connect/read/write)

## Security Considerations

- Network security config allows cleartext for development
- SSL errors are rejected by default (no bypass)
- File access permissions configurable per security needs
- Cookie security maintained through CookieManager

## Dependencies

- **Cronet Embedded**: Chromium networking stack with Brotli/Zstd, HTTP/2, and QUIC
- **OkHttp 5.1.0**: Fallback HTTP client
- **AndroidX WebKit**: Modern WebView APIs
- **Kotlinx Coroutines**: Async operations
- **Koin**: Dependency injection
- **DataStore**: Configuration persistence
- **Compose**: Modern UI framework

## Development Notes

The implementation follows clean architecture principles:
- **Domain**: Pure business logic and entities
- **Data**: Repository pattern with DataStore
- **Presentation**: MVI pattern with StateFlow
- **UI**: Compose with Material 3

All public APIs are documented with KDoc. The codebase uses explicit API mode and follows SOLID principles.
