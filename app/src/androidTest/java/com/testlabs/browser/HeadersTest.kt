/*
 * @author Lorenzo Suarez
 * @date 09/04//2025
 */

package com.testlabs.browser

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.testlabs.browser.domain.settings.WebViewConfig
import org.json.JSONObject
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class HeadersTest {
    @get:Rule
    val rule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun xRequestedWithHeaderNotSent() {
        val latch = CountDownLatch(1)
        val result = arrayOfNulls<String>(1)

        rule.scenario.onActivity { activity ->
            val webView = WebView(activity)
            activity.setContentView(webView)

            // Apply the same X-Requested-With header suppression as our WebViewHost
            webView.settings.javaScriptEnabled = true
            applyRequestedWithHeaderSuppression(webView)

            webView.webViewClient =
                object : WebViewClient() {
                    override fun onPageFinished(
                        view: WebView?,
                        url: String?,
                    ) {
                        view?.evaluateJavascript("document.body.innerText") { value ->
                            result[0] = value
                            latch.countDown()
                        }
                    }
                }
            webView.loadUrl("https://httpbin.org/headers")
        }

        latch.await(15, TimeUnit.SECONDS)

        val text = result[0]?.trim('"')?.replace("\\n", "")?.replace("\\", "") ?: "{}"
        val headers = JSONObject(text).getJSONObject("headers")

        // Verify that X-Requested-With header is NOT present
        assertFalse(
            "X-Requested-With header should not be present in requests to achieve Chrome parity",
            headers.has("X-Requested-With")
        )

        // Verify that other expected headers are present
        assertTrue("User-Agent header should be present", headers.has("User-Agent"))
        assertTrue("Accept header should be present", headers.has("Accept"))
    }

    @Test
    fun requestedWithHeaderControlFeatureSupported() {
        // Test that the WebView feature is supported on this device
        val isAllowListSupported = WebViewFeature.isFeatureSupported(WebViewFeature.REQUESTED_WITH_HEADER_ALLOW_LIST)

        // The allow list method should be supported for the header suppression to work
        assertTrue(
            "REQUESTED_WITH_HEADER_ALLOW_LIST should be supported for X-Requested-With header suppression",
            isAllowListSupported
        )
    }

    @Test
    fun webViewConfigDefaultsToHeaderSuppression() {
        val config = WebViewConfig()
        assertTrue(
            "WebViewConfig should default to disabling X-Requested-With header",
            config.disableXRequestedWithHeader
        )
    }

    private fun applyRequestedWithHeaderSuppression(webView: WebView) {
        var headerSuppressed = false

        // Use REQUESTED_WITH_HEADER_ALLOW_LIST feature to suppress the header
        try {
            if (WebViewFeature.isFeatureSupported(WebViewFeature.REQUESTED_WITH_HEADER_ALLOW_LIST)) {
                // Set empty allow list to block the header for all origins
                WebSettingsCompat.setRequestedWithHeaderOriginAllowList(webView.settings, emptySet())
                headerSuppressed = true
            }
        } catch (e: Exception) {
            // Ignore exceptions in test
        }
    }
}
