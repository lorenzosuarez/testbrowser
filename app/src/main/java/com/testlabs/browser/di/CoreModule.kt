/*
 * @author Lorenzo Suarez
 * @date 09/04//2025
 */

package com.testlabs.browser.di

import com.testlabs.browser.ui.browser.AndroidVersionProvider
import com.testlabs.browser.ui.browser.ChromeUAProvider
import com.testlabs.browser.ui.browser.DefaultDeviceInfoProvider
import com.testlabs.browser.ui.browser.DeviceInfoProvider
import com.testlabs.browser.ui.browser.JsCompatScriptProvider
import com.testlabs.browser.ui.browser.UAProvider
import com.testlabs.browser.ui.browser.VersionProvider
import com.testlabs.browser.network.HttpStackFactory
import com.testlabs.browser.ui.browser.NetworkProxy
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Core module providing shared primitives and utilities.
 */
public val coreModule: Module =
    module {
        single<VersionProvider> { AndroidVersionProvider(androidContext()) }
        single<UAProvider> { ChromeUAProvider(get()) }
        single<DeviceInfoProvider> { DefaultDeviceInfoProvider(androidContext()) }
        single { JsCompatScriptProvider(get()) }
        single { HttpStackFactory.create(androidContext(), get()) }
        single { NetworkProxy(get()) }
    }
