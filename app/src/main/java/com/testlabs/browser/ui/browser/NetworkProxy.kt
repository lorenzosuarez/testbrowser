/**
 * Author: Lorenzo Suarez
 * Date: 09/06/2025
 */
package com.testlabs.browser.ui.browser

import android.content.Context
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import androidx.webkit.WebResourceResponseCompat
import com.testlabs.browser.domain.settings.EngineMode
import com.testlabs.browser.domain.settings.WebViewConfig
import com.testlabs.browser.network.CronetHolder
import com.testlabs.browser.network.CronetHttpStack
import com.testlabs.browser.network.OkHttpStack
import com.testlabs.browser.network.ProxyRequest
import com.testlabs.browser.network.UserAgentClientHintsManager
import kotlinx.coroutines.runBlocking
import java.util.Locale

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
    private val uaProvider: UAProvider,
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

        val headers = LinkedHashMap<String, String>()
        request.requestHeaders.forEach { (k, v) ->
            val lk = k.lowercase(Locale.US)
            if (config.suppressXRequestedWith && lk == "x-requested-with") return@forEach
            when (lk) {
                "user-agent" -> headers["User-Agent"] = userAgent
                "accept-language" -> headers["Accept-Language"] = acceptLanguage
                "sec-ch-ua", "sec-ch-ua-mobile", "sec-ch-ua-platform" -> {}
                else -> headers[k] = v
            }
        }
        headers["User-Agent"] = userAgent
        headers["Accept-Language"] = acceptLanguage

        val major = Regex("Chrome/(\\d+)").find(userAgent)?.groupValues?.get(1) ?: "99"
        val hints = chManager.lowEntropyUaHints(major)
        headers["Sec-CH-UA"] = hints["sec-ch-ua"]!!
        headers["Sec-CH-UA-Mobile"] = hints["sec-ch-ua-mobile"]!!
        headers["Sec-CH-UA-Platform"] = hints["sec-ch-ua-platform"]!!

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
        val webResp = WebResourceResponseCompat.create(mime, charset, resp.statusCode, resp.reasonPhrase, headerMap, resp.body)
        return webResp
    }
}
