package com.testlabs.browser.network

import android.content.Context
import android.util.Log
import com.testlabs.browser.domain.settings.EngineMode
import com.testlabs.browser.domain.settings.WebViewConfig

private const val TAG = "HttpStackFactory"

public object HttpStackFactory {

    public fun create(
        context: Context,
        userAgentProvider: UserAgentProvider,
        uaChManager: UserAgentClientHintsManager,
        config: WebViewConfig
    ): HttpStack {
        Log.d(TAG, "Creating HTTP stack (engine: ${config.engineMode}, proxy: ${config.proxyEnabled}, QUIC: ${config.enableQuic})...")

        return if (config.proxyEnabled) {
            try {
                when (config.engineMode) {
                    EngineMode.Cronet -> {
                        Log.d(TAG, "Using CronetHttpStack with QUIC=${config.enableQuic}")
                        CronetHttpStack(context, userAgentProvider, uaChManager, config.enableQuic)
                    }
                    EngineMode.OkHttp -> {
                        Log.d(TAG, "Using OkHttpStack")
                        OkHttpStack(userAgentProvider, uaChManager)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating ${config.engineMode} stack, using OkHttp fallback", e)
                OkHttpStack(userAgentProvider, uaChManager)
            }
        } else {
            Log.d(TAG, "Proxy disabled, using OkHttp")
            OkHttpStack(userAgentProvider, uaChManager)
        }
    }
}
