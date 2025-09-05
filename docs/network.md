# Network layer notes

## Header policy

For every WebView request intercepted through the OkHttp engine the following headers are enforced:

- `User-Agent`: Chrome stable on Android (major 119).
- `Accept-Language`: `en-US,en;q=0.9` (toggleable to just `en-US`).
- `Accept`: `text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8`.
- `Accept-Encoding`: `gzip, deflate, br, zstd`.
- `Sec-CH-UA`: brands for *Chromium* and *Google Chrome* with the correct major version.
- `Sec-CH-UA-Mobile`: `?1`.
- `Sec-CH-UA-Platform`: `"Android"`.

## Content-Encoding normalisation

Responses are inspected and decoded on magic bytes or the `Content-Encoding` header. Supported encodings:

- gzip `1F 8B`
- brotli (via header)
- zstd `28 B5 2F FD`

After decoding the response headers `Content-Encoding` and `Content-Length` are stripped before handing the result to the WebView.

## Verification

1. **TLS Peet** – load `https://tls.peet.ws/api/all` and check that request headers match the policy above.
2. **FingerprintJS demo** – open `https://fingerprintjs.github.io/fingerprintjs/` and ensure navigator properties reflect Chrome on Android.
3. **BrowserScan** – navigate to `https://browserleaks.com/ip` and confirm that the page loads without `ERR_CONTENT_DECODING_FAILED` and the IP section renders.
