/**
 * Author: Lorenzo Suarez
 * Date: 09/06/2025
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
        val PROXY_ENABLED = booleanPreferencesKey("proxy_enabled")
        val PROXY_INTERCEPT_ENABLED = booleanPreferencesKey("proxy_intercept_enabled")
    }

    override val config: Flow<WebViewConfig> = dataStore.data.map { preferences ->
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
            proxyEnabled = preferences[PreferenceKeys.PROXY_ENABLED] ?: true,
            proxyInterceptEnabled = preferences[PreferenceKeys.PROXY_INTERCEPT_ENABLED] ?: true,
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
            preferences[PreferenceKeys.PROXY_ENABLED] = config.proxyEnabled
            preferences[PreferenceKeys.PROXY_INTERCEPT_ENABLED] = config.proxyInterceptEnabled
        }
    }

    override suspend fun reset() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
