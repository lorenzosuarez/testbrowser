# Test Browser

This sample demonstrates a WebView based browser.

## Client Hint limitations

The `Sec-CH-UA*` headers and `navigator.userAgentData` values are controlled by the
underlying WebView engine and cannot be fully aligned with Chrome. While this project
matches Chrome's user agent string, the client hints may continue to expose different
brands (e.g. `"Not A Brand"`).

## Reproducing header parity checks

1. Build and install the app.
2. Open `https://httpbin.org/headers` and verify that the `X-Requested-With` header is
   absent and that `User-Agent` matches Chrome.
3. Navigate to `https://www.browserscan.net/` and compare the reported
   `navigator.userAgent` with Chrome for further validation.

