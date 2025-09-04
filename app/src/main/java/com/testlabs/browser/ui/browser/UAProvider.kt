package com.testlabs.browser.ui.browser

/**
 * Provides user agent strings that mimic Chrome Mobile behavior for optimal fingerprinting compatibility.
 */
interface UAProvider {

    /**
     * Gets the current user agent string.
     */
    fun getCurrentUserAgent(): String

    /**
     * Switches to desktop user agent mode.
     */
    fun switchToDesktop()

    /**
     * Switches to mobile user agent mode.
     */
    fun switchToMobile()

    /**
     * Checks if currently using desktop user agent.
     */
    fun isDesktopMode(): Boolean
}

/**
 * Default implementation of UAProvider with Chrome Mobile compatibility.
 */
class DefaultUAProvider : UAProvider {

    private var isDesktop = false

    override fun getCurrentUserAgent(): String = if (isDesktop) DESKTOP_UA else MOBILE_UA

    override fun switchToDesktop() {
        isDesktop = true
    }

    override fun switchToMobile() {
        isDesktop = false
    }

    override fun isDesktopMode(): Boolean = isDesktop

    companion object {
        private const val MOBILE_UA = "Mozilla/5.0 (Linux; Android 13; SM-G991B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Mobile Safari/537.36"
        private const val DESKTOP_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36"
    }
}
