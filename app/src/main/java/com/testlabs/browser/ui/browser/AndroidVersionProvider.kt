/**
 * Author: Lorenzo Suarez
 * Date: 09/08/2025
 */

package com.testlabs.browser.ui.browser

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

/**
 * Retrieves version information from the Android system and installed Chrome/WebView packages.
 */
public class AndroidVersionProvider(private val context: Context) : VersionProvider {
    override fun androidVersion(): String = Build.VERSION.RELEASE ?: "0"

    override fun chromeFullVersion(): String {
        
        val chrome = packageVersion("com.android.chrome")
        if (chrome != null) return chrome
        return packageVersion("com.google.android.webview") ?: "0.0.0.0"
    }

    private val major: Int by lazy { chromeFullVersion().substringBefore('.').toIntOrNull() ?: 0 }

    override fun chromeMajor(): Int = major

    override fun deviceModel(): String = Build.MODEL ?: "Android"

    private fun packageVersion(pkg: String): String? {
        return try {
            val info = context.packageManager.getPackageInfo(pkg, 0)
            info.versionName
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }
}
