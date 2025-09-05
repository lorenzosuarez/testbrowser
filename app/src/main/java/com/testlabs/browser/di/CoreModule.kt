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
import com.testlabs.browser.network.HttpStackFactory
import com.testlabs.browser.ui.browser.NetworkProxy
import com.testlabs.browser.network.OkHttpStack
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

private const val TAG = "CoreModule"

/**
 * Core module providing shared primitives and utilities with optimized HTTP stack creation.
 */
public val coreModule: Module =
    module {
        single<VersionProvider> { AndroidVersionProvider(androidContext()) }
        single<UAProvider> { ChromeUAProvider(get()) }
        single<DeviceInfoProvider> { DefaultDeviceInfoProvider(androidContext()) }
        single { JsCompatScriptProvider(get()) }

        // Create HTTP stack with Cronet when proxy is enabled, OkHttp as fallback
        single {
            try {
                Log.d(TAG, "Creating HTTP stack with Cronet support...")
                val stack = HttpStackFactory.create(
                    context = androidContext(),
                    uaProvider = get<UAProvider>(),
                    proxyEnabled = true // Default to Cronet for better CDN compatibility
                )
                Log.d(TAG, "HTTP stack created successfully: ${stack.name}")

                // Log capabilities for debugging
                when (stack.name) {
                    "Cronet" -> Log.i(TAG, "Using Cronet stack - HTTP/2, QUIC, Brotli/Zstd enabled for Chrome compatibility")
                    "OkHttp" -> Log.i(TAG, "Using OkHttp stack - Cronet fallback not available")
                    else -> Log.w(TAG, "Unknown HTTP stack: ${stack.name}")
                }

                stack
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create HTTP stack, using OkHttp fallback", e)
                OkHttpStack()
            }
        }

        // NetworkProxy with enhanced cookie sync and header preservation
        single {
            try {
                val proxy = NetworkProxy(get())
                Log.d(TAG, "NetworkProxy created with stack: ${proxy.stackName}")
                proxy
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create NetworkProxy", e)
                // Fallback with OkHttp
                NetworkProxy(OkHttpStack())
            }
        }
    }
