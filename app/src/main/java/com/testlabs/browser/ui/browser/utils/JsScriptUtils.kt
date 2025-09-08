/**
 * Author: Lorenzo Suarez
 * Date: 09/08/2025
 */
package com.testlabs.browser.ui.browser.utils

import com.testlabs.browser.ui.browser.JsCompatScriptProvider

/**
 * Utilities for handling JavaScript script injection and compatibility.
 */
public object JsScriptUtils {

    /**
     * Gets the document start compatibility script from the provider.
     */
    public fun getDocStartScript(jsCompat: JsCompatScriptProvider): String {
        return jsCompat.getCompatibilityScript()
    }
}
