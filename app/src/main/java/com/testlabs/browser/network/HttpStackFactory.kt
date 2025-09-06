package com.testlabs.browser.network

import android.content.Context
import android.util.Log
import com.testlabs.browser.domain.settings.EngineMode
import com.testlabs.browser.domain.settings.WebViewConfig
import com.testlabs.browser.ui.browser.UAProvider

private const val TAG = "HttpStackFactory"

public object HttpStackFactory {

    public fun create(
        context: Context,
        userAgentProvider: UAProvider,
        userAgentClientHintsManager: UserAgentClientHintsManager,
        config: WebViewConfig
    ): HttpStack {
        Log.d(TAG, "Creating HTTP stack (engine: ${config.engineMode}, proxy: ${config.proxyEnabled}, QUIC: ${config.enableQuic})...")

        return if (config.proxyEnabled) {
            try {
                when (config.engineMode) {
                    EngineMode.Cronet -> {
                        Log.d(TAG, "Using CronetHttpStack with QUIC=${config.enableQuic}")
                        CronetHttpStack(context, userAgentProvider, userAgentClientHintsManager, config.enableQuic)
                    }
                    EngineMode.OkHttp -> {
                        Log.d(TAG, "Using OkHttpStack")
                        OkHttpStack(userAgentProvider, userAgentClientHintsManager)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating ${config.engineMode} stack, using OkHttp fallback", e)
                OkHttpStack(userAgentProvider, userAgentClientHintsManager)
            }
        } else {
            Log.d(TAG, "Proxy disabled, using OkHttp")
            OkHttpStack(userAgentProvider, userAgentClientHintsManager)
        }
    }
}
