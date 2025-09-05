package com.testlabs.browser.js

import android.os.Build
import com.testlabs.browser.network.UserAgentProvider

public class JsBridge(private val ua: UserAgentProvider) {
    public fun script(): String {
        val uaString = ua.get()
        val major = ua.major()
        val full = ua.fullVersion()
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
                        { brand: 'Chromium', version: '$major' },
                        { brand: 'Google Chrome', version: '$major' }
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
                            { brand: 'Chromium', version: '$full' },
                            { brand: 'Google Chrome', version: '$full' }
                        ]
                    })
                };
            })();
        """.trimIndent()
    }
}
