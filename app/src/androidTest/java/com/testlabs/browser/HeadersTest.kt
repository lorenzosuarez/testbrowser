package com.testlabs.browser

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebSettingsCompat.RequestedWithHeaderMode
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
        assertFalse(headers.has("X-Requested-With"))
    }

    @Test
    fun requestedWithHeaderControlFeatureSupported() {
        val supported = WebViewFeature.isFeatureSupported(WebViewFeature.REQUESTED_WITH_HEADER_CONTROL)
        assertTrue(supported)
    }

    @Test
    fun webViewConfigDefaults() {
        val config = WebViewConfig()
        assertTrue(config.jsCompatibilityMode)
        assertTrue(config.acceptLanguages.isNotEmpty())
    }

    private fun applyRequestedWithHeaderSuppression(webView: WebView) {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.REQUESTED_WITH_HEADER_CONTROL)) {
            WebSettingsCompat.setRequestedWithHeaderMode(webView.settings, RequestedWithHeaderMode.NO_HEADER)
        }
    }
}
