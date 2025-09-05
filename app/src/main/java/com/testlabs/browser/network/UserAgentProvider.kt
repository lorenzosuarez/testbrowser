package com.testlabs.browser.network

import android.os.Build

public class UserAgentProvider {
    private val majorVersion: Int = 119
    private val fullVersion: String = "$majorVersion.0.0.0"

    public fun get(): String {
        return "Mozilla/5.0 (Linux; Android ${Build.VERSION.RELEASE}; ${Build.MODEL}) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/$fullVersion Mobile Safari/537.36"
    }

    public fun major(): String = majorVersion.toString()
    public fun fullVersion(): String = fullVersion
}
