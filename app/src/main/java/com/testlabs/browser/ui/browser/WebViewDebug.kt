package com.testlabs.browser.ui.browser

import android.webkit.CookieManager
import android.webkit.WebView
import com.testlabs.browser.domain.settings.WebViewConfig
import org.json.JSONObject

public fun dumpWebViewConfig(webView: WebView, config: WebViewConfig): String {
    val s = webView.settings
    val obj = JSONObject()
    obj.put("userAgent", s.userAgentString)
    obj.put("javascriptEnabled", s.javaScriptEnabled)
    obj.put("domStorageEnabled", s.domStorageEnabled)
    obj.put("databaseEnabled", s.databaseEnabled)
    obj.put("supportMultipleWindows", s.supportMultipleWindows)
    obj.put("javaScriptCanOpenWindowsAutomatically", s.javaScriptCanOpenWindowsAutomatically)
    obj.put("useWideViewPort", s.useWideViewPort)
    obj.put("loadWithOverviewMode", s.loadWithOverviewMode)
    obj.put("mediaPlaybackRequiresUserGesture", s.mediaPlaybackRequiresUserGesture)
    obj.put("mixedContentMode", s.mixedContentMode)
    obj.put("acceptThirdPartyCookies", CookieManager.getInstance().acceptThirdPartyCookies(webView))
    obj.put("hardwareAccelerated", webView.isHardwareAccelerated)
    obj.put("acceptLanguages", config.acceptLanguages)
    return obj.toString()
}
