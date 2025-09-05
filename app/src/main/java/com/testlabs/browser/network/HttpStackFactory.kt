package com.testlabs.browser.network

import android.content.Context
import com.google.android.gms.net.CronetProviderInstaller
import com.google.android.gms.tasks.Tasks
import com.testlabs.browser.ui.browser.UAProvider
import org.chromium.net.CronetEngine

public object HttpStackFactory {
    public fun create(context: Context, uaProvider: UAProvider): HttpStack {
        val engine = buildCronet(context, uaProvider.userAgent(desktop = false))
        return if (engine != null) CronetHttpStack(engine) else OkHttpStack()
    }
    private fun buildCronet(context: Context, ua: String): CronetEngine? {
        return try {
            CronetEngine.Builder(context)
                .enableHttp2(true)
                .enableQuic(true)
                .enableBrotli(true)
                .enableZstd(true)
                .enableHttpCache(CronetEngine.Builder.HTTP_CACHE_IN_MEMORY, 10 * 1024 * 1024)
                .setUserAgent(ua)
                .build()
        } catch (e: Throwable) {
            try {
                Tasks.await(CronetProviderInstaller.installProvider(context))
                CronetEngine.Builder(context)
                    .enableHttp2(true)
                    .enableQuic(true)
                    .enableBrotli(true)
                    .enableZstd(true)
                    .enableHttpCache(CronetEngine.Builder.HTTP_CACHE_IN_MEMORY, 10 * 1024 * 1024)
                    .setUserAgent(ua)
                    .build()
            } catch (_: Throwable) {
                null
            }
        }
    }
}
