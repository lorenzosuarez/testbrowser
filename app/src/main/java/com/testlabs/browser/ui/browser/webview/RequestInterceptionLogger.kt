/**
 * Author: Lorenzo Suarez
 * Date: 09/06/2025
 */
package com.testlabs.browser.ui.browser.webview

import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse

private const val TAG = "WebViewHost"

/**
 * Handles logging for request interception with performance metrics.
 */
public object RequestInterceptionLogger {

    /**
     * Logs request details and intercepts with timing information.
     */
    public fun logAndIntercept(
        request: WebResourceRequest,
        interceptor: () -> WebResourceResponse?
    ): WebResourceResponse? {
        val isMain = request.isForMainFrame
        val url = request.url.toString()
        val method = request.method
        val t0 = System.nanoTime()
        Log.d(TAG, "REQ  main=$isMain  $method $url")

        val resp = try {
            interceptor()
        } catch (t: Throwable) {
            null
        }

        val dt = (System.nanoTime() - t0) / 1e6
        if (resp != null) {
            val code = try { resp.statusCode } catch (_: Throwable) { -1 }
            val mime = try { resp.mimeType } catch (_: Throwable) { null }
            val size = try { resp.data?.available() ?: -1 } catch (_: Throwable) { -1 }
            Log.d(TAG, "HIT  code=$code mime=$mime size~${size}B  (${dt}ms)  $method $url")
        } else {
            Log.d(TAG, "MISS (${dt}ms)  $method $url")
        }
        return resp
    }
}
