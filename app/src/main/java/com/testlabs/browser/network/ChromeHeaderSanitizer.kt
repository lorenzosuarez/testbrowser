/**
 * Author: Lorenzo Suarez
 * Date: 09/08/2025
 */
package com.testlabs.browser.network

public object ChromeHeaderSanitizer {
    private fun isDebug(name: String): Boolean {
        val n = name.lowercase()
        return n == "x-proxy-engine" || n.startsWith("x-proxy-") || n.startsWith("x-debug")
    }

    @JvmStatic
    public fun sanitizeOutgoing(headers: MutableMap<String, String>) {
        headers.keys.removeAll { isDebug(it) }
    }

    @JvmStatic
    public fun sanitizeIncoming(headers: Map<String, List<String>>): Map<String, List<String>> {
        val out = linkedMapOf<String, MutableList<String>>()
        val seen = mutableSetOf<String>()
        headers.forEach { (k, v) ->
            if (k.isNullOrBlank()) return@forEach
            val low = k.lowercase()
            if (isDebug(low)) return@forEach
            val key = out.keys.firstOrNull { it.equals(k, true) }
            if (key != null) {
                out[key]?.addAll(v)
            } else {
                seen.add(low)
                out[k] = v.toMutableList()
            }
        }
        return out.mapValues { it.value.toList() }
    }
}
