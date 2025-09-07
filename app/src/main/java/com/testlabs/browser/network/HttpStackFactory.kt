/**
 * Author: Lorenzo Suarez
 * Date: 06/09/2025
 */

package com.testlabs.browser.network

import android.content.Context
import com.testlabs.browser.settings.DeveloperSettings
import com.testlabs.browser.ui.browser.UAProvider
import org.chromium.net.CronetEngine

/**
 * Factory that selects the HTTP stack at runtime based on developer settings.
 */
public object HttpStackFactory {
    /**
     * Creates the best-fitting HttpStack given current developer settings.
     *
     * @param context Android context for Cronet engine creation.
     * @param settings Developer toggles for Cronet/QUIC.
     * @param uaProvider Provider for UA strings.
     */
    public fun create(
        context: Context,
        settings: DeveloperSettings,
        uaProvider: UAProvider,
        chManager: UserAgentClientHintsManager
    ): HttpStack {
        return if (settings.useCronet.value) {
            val builder = CronetEngine.Builder(context)
            if (settings.enableQuic.value) builder.enableQuic(true)
            builder.enableHttp2(true)
            val engine = builder.build()
            CronetHttpStack(engine, uaProvider, chManager)
        } else {
            OkHttpStack(uaProvider, chManager)
        }
    }
}