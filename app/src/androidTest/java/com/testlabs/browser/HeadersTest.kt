package com.testlabs.browser

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.android.inject
import com.testlabs.browser.ui.browser.UAProvider

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
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
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

        // Ensure X-Requested-With is absent
        assertFalse(headers.has("X-Requested-With"))

        // Verify user agent matches provider
        var expectedUa: String? = null
        rule.scenario.onActivity { activity ->
            val provider by activity.inject<UAProvider>()
            expectedUa = provider.userAgent(desktop = false)
        }
        assertEquals(expectedUa, headers.getString("User-Agent"))
    }
}

