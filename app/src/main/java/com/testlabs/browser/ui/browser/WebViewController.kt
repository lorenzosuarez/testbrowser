/**
 * Author: Lorenzo Suarez
 * Date: 06/09/2025
 */

package com.testlabs.browser.ui.browser

import com.testlabs.browser.domain.settings.WebViewConfig

public interface WebViewController {
    public fun loadUrl(url: String)
    public fun reload()
    public fun goBack()
    public fun goForward()
    public fun recreateWebView()
    public fun clearBrowsingData(done: () -> Unit)
    public fun config(): WebViewConfig
    public fun proxyStackName(): String
    public fun dumpSettings(): String
}
