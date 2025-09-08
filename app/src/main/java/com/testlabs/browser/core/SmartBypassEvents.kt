package com.testlabs.browser.core

public object SmartBypassEvents {
    public fun bypass(reason: String, method: String, url: String): String =
        "BYPASS [$reason] $method $url"

    public fun intercept(method: String, url: String): String =
        "INTERCEPT [proxy] $method $url"

    public fun swBypass(reason: String, method: String, url: String): String =
        "SW_BYPASS [$reason] $method $url"

    public fun swIntercept(method: String, url: String): String =
        "SW_INTERCEPT [proxy] $method $url"

    public fun markTtl(origin: String): String = "MARK_TTL $origin 5m"

    public fun reloadOnce(origin: String): String = "RELOAD_ONCE no-proxy $origin"
}
