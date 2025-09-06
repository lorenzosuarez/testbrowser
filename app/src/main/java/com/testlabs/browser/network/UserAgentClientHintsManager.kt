package com.testlabs.browser.network

import com.testlabs.browser.ui.browser.VersionProvider

/**
 * Builds canonical User-Agent Client Hint headers that mirror Chrome for Android.
 *
 * The returned values are raw strings and must not be pre-escaped. Quote
 * characters are included directly so underlying HTTP stacks can transmit them
 * unchanged.
 */
public class UserAgentClientHintsManager(
    private val versionProvider: VersionProvider
) {
    /** Return Chrome major from the current User-Agent. */
    private fun chromeMajor(): String = versionProvider.major()

    /** Exact Sec-CH-UA header value. */
    public fun secChUa(): String =
        "\"Not;A=Brand\";v=\"99\", \"Google Chrome\";v=\"${chromeMajor()}\", \"Chromium\";v=\"${chromeMajor()}\""

    /** Whether the browser is on a mobile device. */
    public fun secChUaMobile(isMobile: Boolean = true): String = if (isMobile) "?1" else "?0"

    /** Platform identifier for Android devices. */
    public fun secChUaPlatform(): String = "\"Android\""

    /** Convenience helper returning the standard UA-CH map. */
    public fun asMap(isMobile: Boolean = true): Map<String, String> = mapOf(
        "sec-ch-ua" to secChUa(),
        "sec-ch-ua-mobile" to secChUaMobile(isMobile),
        "sec-ch-ua-platform" to secChUaPlatform()
    )
}

