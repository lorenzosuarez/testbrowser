/**
 * Author: Lorenzo Suarez
 * Date: 09/08/2025
 */

package com.testlabs.browser.js

import android.os.Build
import com.testlabs.browser.ui.browser.UAProvider

public open class JsBridge(private val ua: UAProvider) {
    public open fun script(): String {
        val uaString = ua.userAgent(desktop = false)
        val chromeVersion = extractChromeVersion(uaString)
        val fullVersion = "$chromeVersion.0.0.0"
        val model = Build.MODEL.replace("'", "")
        val arch = when {
            Build.SUPPORTED_ABIS.any { it.contains("x86_64") } -> "x86"
            Build.SUPPORTED_ABIS.any { it.contains("arm64") } -> "arm"
            else -> "arm"
        }
        val bitness = if (arch == "x86") "64" else "64"

        return """
            (() => {
              try {
                const d=(o,p,v)=>{try{Object.defineProperty(o,p,{get:()=>v,configurable:true});}catch(e){}};
                d(navigator, 'vendorFlavors', ['chrome']);
                d(navigator, 'pdfViewerEnabled', true);
                d(navigator,'userAgent','$uaString');
                d(navigator,'language','en-US');
                d(navigator,'languages',['en-US','en']);
                d(navigator,'platform','Linux ${if (arch=="x86") "x86_64" else "armv8l"}');
                d(navigator,'vendor','Google Inc.');
                d(navigator,'hardwareConcurrency',${Runtime.getRuntime().availableProcessors().coerceIn(1,8)});
                d(navigator,'deviceMemory',2);
                d(navigator,'maxTouchPoints',5);

                window.chrome = { runtime:{}, app:{}, csi:()=>({}), loadTimes:()=>({}) };

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
                    model: '$model',
                    architecture: '$arch',
                    bitness: '$bitness',
                    wow64: false,
                    uaFullVersion: '$fullVersion',
                    fullVersionList: [
                      { brand: 'Chromium', version: '$fullVersion' },
                      { brand: 'Google Chrome', version: '$fullVersion' }
                    ]
                  })
                };
              } catch(e){}
            })();
        """.trimIndent()
    }

    public fun injectScript(webView: android.webkit.WebView?, url: String) {
        webView?.evaluateJavascript(script(), null)
    }

    private fun extractChromeVersion(userAgent: String): String {
        return runCatching {
            val chromeRegex = Regex("""Chrome/(\d+)""")
            chromeRegex.find(userAgent)?.groupValues?.get(1) ?: "139"
        }.getOrElse { "139" }
    }
}