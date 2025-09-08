/**
 * Author: Lorenzo Suarez
 * Date: 09/08/2025
 */

package com.testlabs.browser.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

public class DeveloperSettings {
    private val _useCronet = MutableStateFlow(false)
    public val useCronet: StateFlow<Boolean> = _useCronet

    private val _enableQuic = MutableStateFlow(false)
    public val enableQuic: StateFlow<Boolean> = _enableQuic

    private val _richAcceptLanguage = MutableStateFlow(true)
    public val richAcceptLanguage: StateFlow<Boolean> = _richAcceptLanguage

    private val _proxyMainDocument = MutableStateFlow(true)
    public val proxyMainDocument: StateFlow<Boolean> = _proxyMainDocument

    private val _debugFeatureLogging = MutableStateFlow(false)
    public val debugFeatureLogging: StateFlow<Boolean> = _debugFeatureLogging

    public fun setUseCronet(value: Boolean): Unit { _useCronet.value = value }
    public fun setEnableQuic(value: Boolean): Unit { _enableQuic.value = value }
    public fun setRichAcceptLanguage(value: Boolean): Unit { _richAcceptLanguage.value = value }
    public fun setProxyMainDocument(value: Boolean): Unit { _proxyMainDocument.value = value }
    public fun setDebugFeatureLogging(value: Boolean): Unit { _debugFeatureLogging.value = value }

    public val debugLoggingEnabled: Boolean = false
    public val performanceMonitoringEnabled: Boolean = false
    public val networkInterceptionLogging: Boolean = false
    public val javascriptErrorReporting: Boolean = true
}
