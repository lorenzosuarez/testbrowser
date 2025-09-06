/**
 * Author: Lorenzo Suarez
 * Date: 09/06/2025
 */
package com.testlabs.browser.ui.browser

/**
 * Controller interface exposed to the UI layer.
 */
public interface WebViewController {
    public fun loadUrl(url: String)
    public fun reload()
    public fun goBack()
    public fun goForward()
    public fun recreateWebView()
    public fun clearBrowsingData(done: () -> Unit)
    public fun requestedWithHeaderMode(): RequestedWithHeaderMode
    public fun proxyStackName(): String
}

public interface FileUploadHandlerOwner {
    public fun attachFileUploadHandler(handler: FileUploadHandler)
}