package com.testlabs.browser.network

import android.content.Context
import android.util.Log
import com.testlabs.browser.ui.browser.UAProvider

private const val TAG = "HttpStackFactory"

public object HttpStackFactory {

    public fun create(context: Context, uaProvider: UAProvider, proxyEnabled: Boolean = true): HttpStack {
        Log.d(TAG, "Creating HTTP stack (proxy enabled: $proxyEnabled)...")

        return if (proxyEnabled) {
            try {
                val userAgent = uaProvider.userAgent(desktop = false)
                val engine = CronetHolder.getEngine(context, userAgent)

                if (engine != null) {
                    Log.d(TAG, "Using CronetHttpStack")
                    CronetHttpStack(engine)
                } else {
                    Log.w(TAG, "Cronet engine creation failed, falling back to OkHttp")
                    OkHttpStack()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating Cronet stack, using OkHttp fallback", e)
                OkHttpStack()
            }
        } else {
            Log.d(TAG, "Proxy disabled, using OkHttp")
            OkHttpStack()
        }
    }
}
