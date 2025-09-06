/**
 * Author: Lorenzo Suarez
 * Date: 09/06/2025
 */
package com.testlabs.browser.ui.browser

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse

public interface NetworkProxy {
    public val stackName: String
    public fun interceptRequest(
        request: WebResourceRequest,
        userAgent: String,
        acceptLanguage: String,
        proxyEnabled: Boolean
    ): WebResourceResponse?
}

public class DefaultNetworkProxy : NetworkProxy {
    override val stackName: String = "Default"

    override fun interceptRequest(
        request: WebResourceRequest,
        userAgent: String,
        acceptLanguage: String,
        proxyEnabled: Boolean
    ): WebResourceResponse? = null
}
