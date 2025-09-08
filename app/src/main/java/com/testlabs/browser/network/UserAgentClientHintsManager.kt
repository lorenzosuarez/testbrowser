/**
 * Author: Lorenzo Suarez
 * Date: 09/08/2025
 */

package com.testlabs.browser.network

import com.testlabs.browser.ui.browser.VersionProvider

/**
 * Builds canonical User-Agent Client Hint headers that mirror Chrome for Android.
 *
 * The returned values include quotes and must be sent verbatim. Values are derived from
 * the provided [VersionProvider] to ensure consistency between classic UA and UA-CH.
 */
public class UserAgentClientHintsManager(
    private val versionProvider: VersionProvider,
    public var enabled: Boolean = true
) {
    /**
     * Returns the Sec-CH-UA brand list using Chrome-style brands and versions.
     *
     * Example:
     * `"Not A;Brand";v="99", "Google Chrome";v="124", "Chromium";v="124"`
     */
    public fun secChUa(): String =
        "\"Not A;Brand\";v=\"99\", " +
                "\"Google Chrome\";v=\"${chromeMajor()}\", " +
                "\"Chromium\";v=\"${chromeMajor()}\""

    /**
     * Returns whether the browser is considered mobile (`?1`) or desktop (`?0`).
     */
    public fun secChUaMobile(isMobile: Boolean = true): String = if (isMobile) "?1" else "?0"

    /**
     * Returns the platform name for Android devices.
     *
     * Example: `"Android"`
     */
    public fun secChUaPlatform(): String = "\"Android\""

    /**
     * Returns the full version list including Chrome and Chromium full versions.
     *
     * Example:
     * `"Not A;Brand";v="99.0.0.0", "Google Chrome";v="124.0.6367.123", "Chromium";v="124.0.6367.123"`
     */
    public fun secChUaFullVersionList(): String =
        "\"Not A;Brand\";v=\"99.0.0.0\", " +
                "\"Google Chrome\";v=\"${chromeFullVersion()}\", " +
                "\"Chromium\";v=\"${chromeFullVersion()}\""

    /**
     * Returns the primary Chrome full version quoted.
     */
    public fun secChUaFullVersion(): String = "\"${chromeFullVersion()}\""

    /**
     * Returns the Android platform version triple quoted.
     *
     * Example: `"16.0.0"`
     */
    public fun secChUaPlatformVersion(): String {
        val raw = versionProvider.androidVersion()
        val triple = if (raw.contains('.')) raw else "$raw.0.0"
        return "\"$triple\""
    }

    /**
     * Returns a complete UA-CH header map suitable for request injection.
     *
     * Includes: `sec-ch-ua`, `sec-ch-ua-mobile`, `sec-ch-ua-platform`,
     * `sec-ch-ua-full-version`, `sec-ch-ua-full-version-list`, and `sec-ch-ua-platform-version`.
     */
    public fun asMap(isMobile: Boolean = true): Map<String, String> =
        if (!enabled) {
            emptyMap()
        } else {
            linkedMapOf(
                "sec-ch-ua" to secChUa(),
                "sec-ch-ua-mobile" to secChUaMobile(isMobile),
                "sec-ch-ua-platform" to secChUaPlatform(),
                "sec-ch-ua-full-version" to secChUaFullVersion(),
                "sec-ch-ua-full-version-list" to secChUaFullVersionList(),
                "sec-ch-ua-platform-version" to secChUaPlatformVersion()
            )
        }

    /**
     * Returns the Chrome major version used for UA-CH brands.
     */
    private fun chromeMajor(): String = versionProvider.major()

    /**
     * Returns the Chrome full version used for UA-CH full-version list.
     */
    private fun chromeFullVersion(): String = versionProvider.chromeFullVersion()
}
