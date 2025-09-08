/**
 * Author: Lorenzo Suarez
 * Date: 09/08/2025
 */
package com.testlabs.browser.ui.browser.utils

import java.util.Locale

/**
 * Utilities for handling device language configuration.
 */
public object DeviceLanguageUtils {

    /**
     * Builds Accept-Language header based on device locale configuration.
     */
    public fun buildDeviceAcceptLanguage(): String {
        val locales = mutableListOf<Locale>()
        val localeList = android.content.res.Resources.getSystem().configuration.locales
        for (i in 0 until localeList.size()) {
            locales.add(localeList[i])
        }
        return locales.mapIndexed { index, locale ->
            val quality = 1.0 - (index * 0.1)
            "${locale.language}-${locale.country};q=${"%.1f".format(quality)}"
        }.joinToString(",")
    }
}
