package com.testlabs.browser.js

import android.os.Build
import com.testlabs.browser.ui.browser.UAProvider

public class JsBridge(private val ua: UAProvider) {
    public fun script(): String {
        val uaString = ua.userAgent(desktop = false)
        val chromeVersion = extractChromeVersion(uaString)
        val fullVersion = "$chromeVersion.0.0.0"

        return """
            (() => {
                Object.defineProperty(navigator, 'userAgent', {get: () => '$uaString'});
                Object.defineProperty(navigator, 'language', {get: () => 'en-US'});
                Object.defineProperty(navigator, 'languages', {get: () => ['en-US','en']});
                Object.defineProperty(navigator, 'platform', {get: () => 'Linux aarch64'});
                Object.defineProperty(navigator, 'vendor', {get: () => 'Google Inc.'});
                window.chrome = { app: {}, runtime: {} };
                navigator.userAgentData = {
                    brands: [
                        { brand: 'Chromium', version: '$chromeVersion' },
                        { brand: 'Google Chrome', version: '$chromeVersion' }
                    ],
                    mobile: true,
                    platform: 'Android',
                    getHighEntropyValues: async () => ({
                        platform: 'Android',
                        platformVersion: '${Build.VERSION.RELEASE}',
                        model: '${Build.MODEL}',
                        architecture: 'arm',
                        bitness: '64',
                        wow64: false,
                        fullVersionList: [
                            { brand: 'Chromium', version: '$fullVersion' },
                            { brand: 'Google Chrome', version: '$fullVersion' }
                        ]
                    })
                };
            })();
        """.trimIndent()
    }

    private fun extractChromeVersion(userAgent: String): String {
        return runCatching {
            val chromeRegex = Regex("""Chrome/(\d+)""")
            chromeRegex.find(userAgent)?.groupValues?.get(1) ?: "119"
        }.getOrElse { "119" }
    }
}
