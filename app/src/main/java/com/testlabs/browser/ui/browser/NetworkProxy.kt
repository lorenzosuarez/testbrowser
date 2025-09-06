/**
 * Author: Lorenzo Suarez
 * Date: 09/06/2025
 */
package com.testlabs.browser.ui.browser

import android.content.Context
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import com.testlabs.browser.domain.settings.EngineMode
import com.testlabs.browser.domain.settings.WebViewConfig
import com.testlabs.browser.network.CronetHolder
import com.testlabs.browser.network.CronetHttpStack
import com.testlabs.browser.network.OkHttpStack
import com.testlabs.browser.network.ProxyRequest
import com.testlabs.browser.network.UserAgentClientHintsManager
import kotlinx.coroutines.runBlocking

public interface NetworkProxy {
    public val stackName: String
    public fun interceptRequest(
        request: WebResourceRequest,
        userAgent: String,
        acceptLanguage: String,
        proxyEnabled: Boolean
    ): WebResourceResponse?
}

public class DefaultNetworkProxy(
    context: Context,
    private val config: WebViewConfig,
    uaProvider: UAProvider,
    private val chManager: UserAgentClientHintsManager
) : NetworkProxy {
    private val httpStack = if (config.engineMode == EngineMode.Cronet) {
        val ua = config.customUserAgent ?: uaProvider.userAgent(desktop = config.desktopMode)
        val engine = CronetHolder.getEngine(context, ua)
        if (engine != null) CronetHttpStack(engine, uaProvider, chManager)
        else OkHttpStack(uaProvider, chManager)
    } else {
        OkHttpStack(uaProvider, chManager)
    }

    override val stackName: String = httpStack.name

    override fun interceptRequest(
        request: WebResourceRequest,
        userAgent: String,
        acceptLanguage: String,
        proxyEnabled: Boolean
    ): WebResourceResponse? {
        if (!proxyEnabled) return null

        val headers = normalizeHeaders(request.requestHeaders).toMutableMap()
        headers["User-Agent"] = userAgent
        headers["Accept-Language"] = acceptLanguage

        val proxyReq = ProxyRequest(
            url = request.url.toString(),
            method = request.method,
            headers = headers
        )

        val resp = runBlocking { httpStack.execute(proxyReq) }
        val headerMap = resp.headers.mapValues { it.value.joinToString(",") }
        val contentType = headerMap["Content-Type"] ?: "application/octet-stream"
        val mime = contentType.substringBefore(';')
        val charset = contentType.substringAfter("charset=", "")
            .ifEmpty { null }

        val webResp = WebResourceResponse(mime, charset, resp.body)
        webResp.responseHeaders = headerMap.toMutableMap()
        webResp.setStatusCodeAndReasonPhrase(resp.statusCode, resp.reasonPhrase.ifBlank { " " })
        return webResp
}

    /** Normalize outbound headers from WebView prior to hitting HttpStack */
    public fun normalizeHeaders(incoming: Map<String, String>): Map<String, String> {
        val sanitized = incoming.toMutableMap()

        if (config.suppressXRequestedWith) {
            sanitized.keys
                .filter { it.equals("x-requested-with", ignoreCase = true) }
                .forEach { sanitized.remove(it) }
        }

        listOf("sec-ch-ua", "sec-ch-ua-mobile", "sec-ch-ua-platform").forEach { k ->
            sanitized.keys
                .firstOrNull { it.equals(k, ignoreCase = true) }
                ?.let { sanitized.remove(it) }
        }

        if (chManager.enabled) {
            sanitized["sec-ch-ua"] = chManager.secChUa()
            sanitized["sec-ch-ua-mobile"] = chManager.secChUaMobile(true)
            sanitized["sec-ch-ua-platform"] = chManager.secChUaPlatform()
        }

        return sanitized
    }
}
