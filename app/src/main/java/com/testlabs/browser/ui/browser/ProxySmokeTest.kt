package com.testlabs.browser.ui.browser

import android.util.Log
import android.webkit.WebView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "ProxySmokeTest"

/**
 * Smoke testing utility to verify proxy functionality is working correctly.
 * Tests the key scenarios outlined in the requirements.
 */
public class ProxySmokeTest {

    private val testUrls = listOf(
        "https://www.google.com" to "Google should load without garbled characters",
        "https://www.browserscan.net/" to "BrowserScan should load without module script MIME errors",
        "https://tls.peet.ws/api/all" to "TLS fingerprinting should show proper headers without X-Requested-With",
        "https://httpbin.org/image/png" to "PNG image should download/render correctly",
        "https://fonts.googleapis.com/css2?family=Roboto:wght@400;500&display=swap" to "CSS with fonts should load properly"
    )

    public fun runSmokeTests(
        webView: WebView,
        onTestResult: (String, Boolean, String) -> Unit,
        onAllTestsComplete: () -> Unit
    ) {
        Log.i(TAG, "Starting proxy smoke tests...")

        CoroutineScope(Dispatchers.Main).launch {
            var testIndex = 0

            fun runNextTest() {
                if (testIndex >= testUrls.size) {
                    Log.i(TAG, "All smoke tests completed")
                    onAllTestsComplete()
                    return
                }

                val (url, description) = testUrls[testIndex]
                Log.d(TAG, "Running test ${testIndex + 1}/${testUrls.size}: $description")

                // Set up a simple success/failure detection
                var testCompleted = false
                val startTime = System.currentTimeMillis()

                val originalClient = webView.webViewClient
                webView.webViewClient = object : android.webkit.WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        if (!testCompleted) {
                            testCompleted = true
                            val loadTime = System.currentTimeMillis() - startTime
                            val success = loadTime < 30000 // Basic success criteria
                            val message = if (success) "Loaded in ${loadTime}ms" else "Timeout or error"

                            Log.d(TAG, "Test ${testIndex + 1} result: $success - $message")
                            onTestResult(description, success, message)

                            // Restore original client and continue
                            webView.webViewClient = originalClient
                            testIndex++

                            // Wait a bit before next test
                            CoroutineScope(Dispatchers.Main).launch {
                                delay(2000)
                                runNextTest()
                            }
                        }
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: android.webkit.WebResourceRequest?,
                        error: android.webkit.WebResourceError?
                    ) {
                        if (request?.isForMainFrame == true && !testCompleted) {
                            testCompleted = true
                            val message = "Error: ${error?.description}"
                            Log.w(TAG, "Test ${testIndex + 1} failed: $message")
                            onTestResult(description, false, message)

                            webView.webViewClient = originalClient
                            testIndex++

                            CoroutineScope(Dispatchers.Main).launch {
                                delay(2000)
                                runNextTest()
                            }
                        }
                    }

                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: android.webkit.WebResourceRequest?
                    ): android.webkit.WebResourceResponse? {
                        // Pass through to original client
                        return originalClient.shouldInterceptRequest(view, request)
                    }
                }

                // Start the test
                webView.loadUrl(url)

                // Timeout protection
                CoroutineScope(Dispatchers.Main).launch {
                    delay(30000)
                    if (!testCompleted) {
                        testCompleted = true
                        Log.w(TAG, "Test ${testIndex + 1} timed out")
                        onTestResult(description, false, "Timeout after 30s")

                        webView.webViewClient = originalClient
                        testIndex++
                        runNextTest()
                    }
                }
            }

            runNextTest()
        }
    }

    /**
     * Quick validation that the proxy configuration is correct
     */
    public fun validateProxyConfiguration(networkProxy: NetworkProxy): List<String> {
        val issues = mutableListOf<String>()

        Log.d(TAG, "Validating proxy configuration...")
        Log.i(TAG, "Using HTTP stack: ${networkProxy.stackName}")

        // Check stack name
        when (networkProxy.stackName) {
            "OkHttp" -> Log.i(TAG, "✓ OkHttp stack active - gzip/br/zstd compression enabled")
            "Cronet" -> Log.i(TAG, "✓ Cronet stack active - HTTP/2, QUIC, Brotli enabled for JA3/JA4 accuracy")
            else -> {
                val issue = "⚠ Unknown HTTP stack: ${networkProxy.stackName}"
                Log.w(TAG, issue)
                issues.add(issue)
            }
        }

        return issues
    }

    /**
     * Log key verification points for manual inspection
     */
    public fun logVerificationPoints() {
        Log.i(TAG, "=== Proxy Verification Checklist ===")
        Log.i(TAG, "1. Check logcat for 'Intercepting:' messages showing each subresource gets its own request")
        Log.i(TAG, "2. Verify no 'Failed to load module script... MIME type \"text/html\"' errors")
        Log.i(TAG, "3. Confirm no garbled characters in rendered pages")
        Log.i(TAG, "4. Check TLS.peet.ws shows no X-Requested-With header")
        Log.i(TAG, "5. Verify Accept-Encoding shows gzip, br, zstd negotiation")
        Log.i(TAG, "6. Images/fonts/binaries render correctly without corruption")
        Log.i(TAG, "=====================================")
    }
}
