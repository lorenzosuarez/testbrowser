/*
 * @author Lorenzo Suarez
 * @date 09/04//2025
 */

package com.testlabs.browser.di

import android.util.Log
import com.testlabs.browser.ui.browser.AndroidVersionProvider
import com.testlabs.browser.ui.browser.ChromeUAProvider
import com.testlabs.browser.ui.browser.DefaultDeviceInfoProvider
import com.testlabs.browser.ui.browser.DeviceInfoProvider
import com.testlabs.browser.ui.browser.JsCompatScriptProvider
import com.testlabs.browser.ui.browser.UAProvider
import com.testlabs.browser.ui.browser.VersionProvider
import com.testlabs.browser.ui.browser.NetworkProxy
import com.testlabs.browser.ui.browser.ChromeCompatibilityInjector
import com.testlabs.browser.network.HttpStack
import com.testlabs.browser.network.UserAgentClientHintsManager
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

private const val TAG = "CoreModule"

/**
 * Core module providing shared primitives and utilities with Chrome Mobile compatibility.
 */
public val coreModule: Module =
    module {
        // Enhanced UA and device info providers
        single<VersionProvider> { AndroidVersionProvider(androidContext()) }
        single<UAProvider> { ChromeUAProvider(get()) }
        single<DeviceInfoProvider> { DefaultDeviceInfoProvider(androidContext()) }
        single { JsCompatScriptProvider(get()) }

        // Chrome Mobile compatibility components
        single { UserAgentClientHintsManager(androidContext()) }
        single { ChromeCompatibilityInjector(get<UAProvider>()) }

        // Network stack with Chrome Mobile compatibility
        single<HttpStack> {
            try {
                Log.d(TAG, "Creating enhanced HTTP stack with Chrome compatibility...")
                val uaProvider = get<UAProvider>()
                val uaChManager = get<UserAgentClientHintsManager>()

                // For now, use OkHttp as default since we need to update HttpStackFactory
                // TODO: Update based on WebViewConfig when available
                val stack = com.testlabs.browser.network.OkHttpStack(uaProvider, uaChManager)

                Log.d(TAG, "HTTP stack created successfully: ${stack.name}")
                stack
            } catch (e: Exception) {
                Log.e(TAG, "Error creating HTTP stack", e)
                // Fallback to basic OkHttp if enhanced version fails
                com.testlabs.browser.network.OkHttpStack(get<UAProvider>(), get<UserAgentClientHintsManager>())
            }
        }

        single { NetworkProxy(get()) }
    }
