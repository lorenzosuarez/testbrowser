package com.testlabs.browser.ui.browser

/**
 * Provides user agent strings matching Chrome for both mobile and desktop modes.
 */
public interface UAProvider {
    /**
     * Returns a user agent string for the requested mode.
     */
    public fun userAgent(desktop: Boolean): String
}

/**
 * Default implementation supplying Chrome user agents.
 */
public class DefaultUAProvider : UAProvider {
    override fun userAgent(desktop: Boolean): String = if (desktop) DESKTOP_UA else MOBILE_UA

    private companion object {
        private const val MOBILE_UA = "Mozilla/5.0 (Linux; Android 13; SM-G991B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Mobile Safari/537.36"
        private const val DESKTOP_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36"
    }
}
