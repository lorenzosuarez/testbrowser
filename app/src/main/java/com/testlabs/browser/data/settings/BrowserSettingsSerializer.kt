/*
 * @author Lorenzo Suarez
 * @date 09/04//2025
 */

package com.testlabs.browser.data.settings

import androidx.datastore.core.Serializer
import com.testlabs.browser.domain.settings.WebViewConfig
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

/**
 * DataStore serializer using JSON for [WebViewConfig].
 */
public object BrowserSettingsSerializer : Serializer<WebViewConfig> {
    override val defaultValue: WebViewConfig = WebViewConfig()

    public override suspend fun readFrom(input: InputStream): WebViewConfig =
        Json.decodeFromString(WebViewConfig.serializer(), input.readBytes().decodeToString())

    override suspend fun writeTo(
        t: WebViewConfig,
        output: OutputStream,
    ): Unit = output.write(Json.encodeToString(WebViewConfig.serializer(), t).encodeToByteArray())
}
