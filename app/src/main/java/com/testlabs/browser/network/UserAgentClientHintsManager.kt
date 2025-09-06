/**
 * Author: Lorenzo Suarez
 * Date: 09/06/2025
 */
package com.testlabs.browser.network

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.concurrent.ConcurrentHashMap

/**
 * UA-CH (Client Hints) management
 * Send high-entropy hints only to origins that previously advertised them via Accept-CH.
 * Persist per-origin flags in DataStore.
 */
public class UserAgentClientHintsManager(private val context: Context) {

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ua_ch_prefs")

    
    private val allowedHints = ConcurrentHashMap<String, Set<String>>()

    public companion object {
        private val ACCEPT_CH_KEY = stringSetPreferencesKey("accept_ch_origins")

        
        private val HIGH_ENTROPY_HINTS = setOf(
            "sec-ch-ua-arch",
            "sec-ch-ua-bitness",
            "sec-ch-ua-model",
            "sec-ch-ua-platform-version",
            "sec-ch-ua-full-version-list"
        )
    }

    /**
     * Returns the default low-entropy UA-CH headers that mimic Chrome Mobile.
     * The provided [majorVersion] must match the major version used in the User-Agent string.
     */
    public fun lowEntropyUaHints(majorVersion: String): Map<String, String> {
        val ua = "\"Not;A=Brand\";v=\"99\", \"Google Chrome\";v=\"$majorVersion\", \"Chromium\";v=\"$majorVersion\""
        return mapOf(
            "sec-ch-ua" to ua,
            "sec-ch-ua-mobile" to "?1",
            "sec-ch-ua-platform" to "\"Android\""
        )
    }

    /**
     * Check if a high-entropy hint is allowed for the given origin
     */
    public suspend fun isHighEntropyAllowed(origin: String, hintName: String): Boolean {
        if (hintName.lowercase() !in HIGH_ENTROPY_HINTS) {
            return true 
        }

        
        val cachedHints = allowedHints[origin]
        if (cachedHints != null) {
            return hintName.lowercase() in cachedHints
        }

        
        val allowedForOrigin = loadAllowedHints(origin)
        allowedHints[origin] = allowedForOrigin

        return hintName.lowercase() in allowedForOrigin
    }

    /**
     * Process Accept-CH header from response and update allowed hints for origin
     */
    public suspend fun processAcceptCH(origin: String, acceptChHeader: String?) {
        if (acceptChHeader.isNullOrBlank()) return

        val requestedHints = acceptChHeader
            .split(",")
            .map { it.trim().lowercase() }
            .filter { it in HIGH_ENTROPY_HINTS }
            .toSet()

        if (requestedHints.isNotEmpty()) {
            
            allowedHints[origin] = requestedHints

            
            saveAllowedHints(origin, requestedHints)
        }
    }

    private suspend fun loadAllowedHints(origin: String): Set<String> {
        return try {
            val originKey = stringSetPreferencesKey("hints_$origin")
            context.dataStore.data.map { preferences ->
                preferences[originKey] ?: emptySet()
            }.first()
        } catch (e: Exception) {
            emptySet()
        }
    }

    private suspend fun saveAllowedHints(origin: String, hints: Set<String>) {
        try {
            val originKey = stringSetPreferencesKey("hints_$origin")
            context.dataStore.edit { preferences ->
                preferences[originKey] = hints
            }
        } catch (e: Exception) {
            
        }
    }

    /**
     * Clear all stored UA-CH permissions (for debugging/reset)
     */
    public suspend fun clearAllHints() {
        try {
            context.dataStore.edit { preferences ->
                preferences.clear()
            }
            allowedHints.clear()
        } catch (e: Exception) {
            
        }
    }
}
