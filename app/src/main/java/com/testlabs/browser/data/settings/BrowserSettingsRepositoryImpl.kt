/**
 * Author: Lorenzo Suarez
 * Date: 06/09/2025
 */

package com.testlabs.browser.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.testlabs.browser.domain.settings.AcceptLanguageMode
import com.testlabs.browser.domain.settings.BrowserSettingsRepository
import com.testlabs.browser.domain.settings.WebViewConfig
import com.testlabs.browser.domain.settings.EngineMode
import com.testlabs.browser.domain.settings.RequestedWithHeaderMode
import com.testlabs.browser.domain.settings.parseRequestedWithHeaderAllowList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * [BrowserSettingsRepository] implementation backed by DataStore.
 */
public class BrowserSettingsRepositoryImpl(
    private val dataStore: DataStore<Preferences>
) : BrowserSettingsRepository {

    private object PreferenceKeys {
        val JAVASCRIPT_ENABLED = booleanPreferencesKey("javascript_enabled")
        val DOM_STORAGE_ENABLED = booleanPreferencesKey("dom_storage_enabled")
        val FILE_ACCESS_ENABLED = booleanPreferencesKey("file_access_enabled")
        val MEDIA_AUTOPLAY_ENABLED = booleanPreferencesKey("media_autoplay_enabled")
        val MIXED_CONTENT_ALLOWED = booleanPreferencesKey("mixed_content_allowed")
        val THIRD_PARTY_COOKIES = booleanPreferencesKey("third_party_cookies")
        val DESKTOP_MODE = booleanPreferencesKey("desktop_mode")
        val CUSTOM_USER_AGENT = stringPreferencesKey("custom_user_agent")
        val ACCEPT_LANGUAGES = stringPreferencesKey("accept_languages")
        val ACCEPT_LANGUAGE_MODE = stringPreferencesKey("accept_language_mode")
        val JS_COMPATIBILITY_MODE = booleanPreferencesKey("js_compatibility_mode")
        val FORCE_DARK_MODE = booleanPreferencesKey("force_dark_mode")
        val SMART_PROXY = booleanPreferencesKey("smart_proxy")
        val PROXY_ENABLED = booleanPreferencesKey("proxy_enabled")
        val PROXY_INTERCEPT_ENABLED = booleanPreferencesKey("proxy_intercept_enabled")
        val REQUESTED_WITH_HEADER_MODE = stringPreferencesKey("requested_with_header_mode")
        val REQUESTED_WITH_HEADER_ALLOW_LIST = stringPreferencesKey("requested_with_header_allow_list")
        val ENGINE_MODE = stringPreferencesKey("engine_mode")
    }

    override val config: Flow<WebViewConfig> = dataStore.data.map { preferences ->
        // Unificamos a un único estado; migración: si no hay SMART_PROXY, usamos PROXY_ENABLED o true.
        val smart = preferences[PreferenceKeys.SMART_PROXY]
            ?: preferences[PreferenceKeys.PROXY_ENABLED]
            ?: true

        WebViewConfig(
            javascriptEnabled = preferences[PreferenceKeys.JAVASCRIPT_ENABLED] ?: true,
            domStorageEnabled = preferences[PreferenceKeys.DOM_STORAGE_ENABLED] ?: true,
            fileAccessEnabled = preferences[PreferenceKeys.FILE_ACCESS_ENABLED] ?: false,
            mediaAutoplayEnabled = preferences[PreferenceKeys.MEDIA_AUTOPLAY_ENABLED] ?: false,
            mixedContentAllowed = preferences[PreferenceKeys.MIXED_CONTENT_ALLOWED] ?: false,
            enableThirdPartyCookies = preferences[PreferenceKeys.THIRD_PARTY_COOKIES] ?: true,
            desktopMode = preferences[PreferenceKeys.DESKTOP_MODE] ?: false,
            customUserAgent = preferences[PreferenceKeys.CUSTOM_USER_AGENT],
            acceptLanguages = preferences[PreferenceKeys.ACCEPT_LANGUAGES] ?: "en-US,en;q=0.9",
            acceptLanguageMode = preferences[PreferenceKeys.ACCEPT_LANGUAGE_MODE]?.let {
                AcceptLanguageMode.valueOf(it)
            } ?: AcceptLanguageMode.Baseline,
            jsCompatibilityMode = preferences[PreferenceKeys.JS_COMPATIBILITY_MODE] ?: true,
            forceDarkMode = preferences[PreferenceKeys.FORCE_DARK_MODE] ?: false,
            smartProxy = smart,
            requestedWithHeaderMode = preferences[PreferenceKeys.REQUESTED_WITH_HEADER_MODE]
                ?.let { RequestedWithHeaderMode.valueOf(it) }
                ?: RequestedWithHeaderMode.ELIMINATED,
            requestedWithHeaderAllowList = preferences[PreferenceKeys.REQUESTED_WITH_HEADER_ALLOW_LIST]
                ?.let { parseRequestedWithHeaderAllowList(it) } ?: emptySet(),
            engineMode = preferences[PreferenceKeys.ENGINE_MODE]
                ?.let { EngineMode.valueOf(it) } ?: EngineMode.Cronet,
        )
    }

    override suspend fun save(config: WebViewConfig) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.JAVASCRIPT_ENABLED] = config.javascriptEnabled
            preferences[PreferenceKeys.DOM_STORAGE_ENABLED] = config.domStorageEnabled
            preferences[PreferenceKeys.FILE_ACCESS_ENABLED] = config.fileAccessEnabled
            preferences[PreferenceKeys.MEDIA_AUTOPLAY_ENABLED] = config.mediaAutoplayEnabled
            preferences[PreferenceKeys.MIXED_CONTENT_ALLOWED] = config.mixedContentAllowed
            preferences[PreferenceKeys.THIRD_PARTY_COOKIES] = config.enableThirdPartyCookies
            preferences[PreferenceKeys.DESKTOP_MODE] = config.desktopMode
            config.customUserAgent?.let { preferences[PreferenceKeys.CUSTOM_USER_AGENT] = it }
            preferences[PreferenceKeys.ACCEPT_LANGUAGES] = config.acceptLanguages
            preferences[PreferenceKeys.ACCEPT_LANGUAGE_MODE] = config.acceptLanguageMode.name
            preferences[PreferenceKeys.JS_COMPATIBILITY_MODE] = config.jsCompatibilityMode
            preferences[PreferenceKeys.FORCE_DARK_MODE] = config.forceDarkMode
            preferences[PreferenceKeys.SMART_PROXY] = config.smartProxy
            preferences[PreferenceKeys.PROXY_ENABLED] = config.smartProxy
            preferences[PreferenceKeys.PROXY_INTERCEPT_ENABLED] = config.smartProxy
            preferences[PreferenceKeys.REQUESTED_WITH_HEADER_MODE] = config.requestedWithHeaderMode.name
            preferences[PreferenceKeys.REQUESTED_WITH_HEADER_ALLOW_LIST] =
                config.requestedWithHeaderAllowList.joinToString(",")
            preferences[PreferenceKeys.ENGINE_MODE] = config.engineMode.name
        }
    }

    override suspend fun reset() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
