package com.testlabs.browser.core

public object SmartBypassEvents {
    private fun withFeat(base: String, feat: String?): String =
        if (feat.isNullOrBlank()) base else "$base $feat"

    public fun bypass(reason: String, method: String, url: String, feat: String? = null): String =
        withFeat("BYPASS [$reason] $method $url", feat)

    public fun intercept(method: String, url: String, feat: String? = null): String =
        withFeat("INTERCEPT [proxy] $method $url", feat)

    public fun swBypass(reason: String, method: String, url: String, feat: String? = null): String =
        withFeat("SW_BYPASS [$reason] $method $url", feat)

    public fun swIntercept(method: String, url: String, feat: String? = null): String =
        withFeat("SW_INTERCEPT [proxy] $method $url", feat)

    public fun markTtl(origin: String): String = "MARK_TTL $origin 5m"

    public fun reloadOnce(origin: String): String = "RELOAD_ONCE no-proxy $origin"
}
