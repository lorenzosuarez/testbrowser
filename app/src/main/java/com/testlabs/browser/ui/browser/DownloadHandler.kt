package com.testlabs.browser.ui.browser

import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.webkit.URLUtil
import android.webkit.WebView
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Revolutionary download handler that intercepts blob creation instead of trying to fetch expired URLs.
 */
public class DownloadHandler(
    private val context: Context,
) {
    private companion object {
        private const val TAG = "DownloadHandler"
        private const val ID_LEN = 8
        private const val DESC = "Downloading"
    }

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    private val extensionMap =
        mapOf(
            "image/png" to "png",
            "image/jpeg" to "jpg",
            "image/gif" to "gif",
            "image/svg+xml" to "svg",
            "text/plain" to "txt",
            "text/html" to "html",
            "application/pdf" to "pdf",
            "application/json" to "json",
            "application/xml" to "xml",
        )

    public fun handleDownload(
        url: String,
        userAgent: String,
        contentDisposition: String,
        mimeType: String,
        webView: WebView,
        onError: (String) -> Unit,
    ) {
        Log.d(TAG, "🚀 Starting download for: $url")
        Log.d(TAG, "MIME: $mimeType, Disposition: $contentDisposition")

        when {
            url.startsWith("http://") || url.startsWith("https://") -> {
                Log.d(TAG, "📡 Using DownloadManager for HTTP URL")
                handleHttpDownload(url, userAgent, contentDisposition, mimeType)
            }
            url.startsWith("blob:") -> {
                Log.d(TAG, "🎯 Using blob interception strategy")
                handleBlobWithInterception(url, mimeType, webView, onError)
            }
            else -> {
                Log.e(TAG, "❌ Unsupported URL scheme: $url")
                onError("Unsupported URL scheme")
            }
        }
    }

    private fun handleHttpDownload(
        url: String,
        userAgent: String,
        contentDisposition: String,
        mimeType: String,
    ) {
        try {
            val filename = URLUtil.guessFileName(url, contentDisposition, mimeType)
            Log.d(TAG, "📂 Download filename: $filename")

            val request =
                DownloadManager.Request(url.toUri()).apply {
                    addRequestHeader("User-Agent", userAgent)
                    setTitle(filename)
                    setDescription(DESC)
                    setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
                    setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
                }

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val downloadId = downloadManager.enqueue(request)
            Log.d(TAG, "✅ HTTP download started with ID: $downloadId")
        } catch (e: Exception) {
            Log.e(TAG, "❌ HTTP download failed", e)
        }
    }

    private fun handleBlobWithInterception(
        url: String,
        mimeType: String,
        webView: WebView,
        onError: (String) -> Unit,
    ) {
        Log.d(TAG, "🔄 Implementing blob interception strategy")

        val interceptorName = "DownloadInterceptor_${System.currentTimeMillis()}"

        val jsInterface =
            object {
                @android.webkit.JavascriptInterface
                fun downloadBlob(
                    data: String,
                    filename: String,
                    mimeType: String,
                ) {
                    Log.d(TAG, "✅ Blob intercepted: $filename ($mimeType)")
                    processDataUrlAsync(data, mimeType)
                }

                @android.webkit.JavascriptInterface
                fun log(message: String) {
                    Log.d(TAG, "JS: $message")
                }
            }

        webView.addJavascriptInterface(jsInterface, interceptorName)

        val script =
            """
            (function() {
                console.log('Installing blob download interceptor...');
                window['$interceptorName'].log('Interceptor installed');
                
                const originalCreateObjectURL = URL.createObjectURL;
                URL.createObjectURL = function(blob) {
                    const url = originalCreateObjectURL.call(this, blob);
                    console.log('Blob URL created:', url);
                    
                    if (url === '$url') {
                        console.log('Target blob detected, processing...');
                        const reader = new FileReader();
                        reader.onload = function() {
                            const filename = 'download_${UUID.randomUUID().toString().take(6)}.${extensionMap[mimeType] ?: "bin"}';
                            window['$interceptorName'].downloadBlob(reader.result, filename, '$mimeType');
                        };
                        reader.onerror = function() {
                            console.error('FileReader error');
                        };
                        reader.readAsDataURL(blob);
                    }
                    
                    return url;
                };
                
                console.log('Blob interceptor ready');
            })();
            """.trimIndent()

        webView.evaluateJavascript(script) { result ->
            Log.d(TAG, "📡 Blob interceptor installed: $result")

            webView.evaluateJavascript("console.log('Interceptor active');") { _ ->

                webView.postDelayed({
                    Log.d(TAG, "🔄 Falling back to manual blob handling")
                    handleBlobManually(url, mimeType, webView, onError, interceptorName)
                }, 2000)
            }
        }
    }

    private fun handleBlobManually(
        url: String,
        mimeType: String,
        webView: WebView,
        onError: (String) -> Unit,
        interceptorName: String,
    ) {
        val script =
            """
            (function() {
                try {
                    const blobUrl = '$url';
                    window['$interceptorName'].log('Attempting manual blob recovery for: ' + blobUrl);
                    
                    if (window.lastBlobData) {
                        window['$interceptorName'].downloadBlob(window.lastBlobData, 'manual_download.${extensionMap[mimeType] ?: "bin"}', '$mimeType');
                        return 'SUCCESS';
                    }
                    
                    const pageText = document.documentElement.outerHTML;
                    if (pageText.includes('data:')) {
                        const dataUrlMatch = pageText.match(/data:[^"']*/);
                        if (dataUrlMatch) {
                            window['$interceptorName'].downloadBlob(dataUrlMatch[0], 'extracted_download.${extensionMap[mimeType] ?: "bin"}', '$mimeType');
                            return 'EXTRACTED';
                        }
                    }
                    
                    return 'NO_DATA_FOUND';
                } catch (e) {
                    window['$interceptorName'].log('Manual recovery error: ' + e.message);
                    return 'ERROR:' + e.message;
                }
            })();
            """.trimIndent()

        webView.evaluateJavascript(script) { result ->
            Log.d(TAG, "📋 Manual blob recovery result: $result")

            if (result?.contains("SUCCESS") == true || result?.contains("EXTRACTED") == true) {
                Log.d(TAG, "✅ Manual blob recovery successful")
            } else {
                Log.e(TAG, "❌ All blob strategies failed")
                onError("Unable to download blob content - URL may have expired")
            }

            webView.removeJavascriptInterface(interceptorName)
        }
    }

    private fun processDataUrlAsync(
        dataUrl: String,
        mimeType: String,
    ) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                Log.d(TAG, "🔄 Processing intercepted data URL (${dataUrl.length} chars)...")

                val commaIndex = dataUrl.indexOf(',')
                if (commaIndex == -1) {
                    Log.e(TAG, "❌ Invalid data URL format")
                    return@launch
                }

                val header = dataUrl.substring(0, commaIndex)
                val data = dataUrl.substring(commaIndex + 1)
                val isBase64 = header.contains("base64")

                Log.d(TAG, "📊 Data URL - Base64: $isBase64, Data size: ${data.length}")

                val bytes =
                    if (isBase64) {
                        android.util.Base64.decode(data, android.util.Base64.DEFAULT)
                    } else {
                        data.toByteArray()
                    }

                val filename = generateFilename(mimeType)
                Log.d(TAG, "💾 Saving as: $filename (${bytes.size} bytes)")

                saveFile(filename, bytes, mimeType)
                withContext(Dispatchers.Main) {
                    Log.d(TAG, "✅ File saved successfully!")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to process intercepted data URL", e)
            }
        }
    }

    private suspend fun saveFile(
        filename: String,
        data: ByteArray,
        mimeType: String,
    ) {
        withContext(Dispatchers.IO) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveToMediaStore(filename, data, mimeType)
            } else {
                saveToLegacyStorage(filename, data)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveToMediaStore(
        filename: String,
        data: ByteArray,
        mimeType: String,
    ) {
        val values =
            ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }

        val resolver = context.contentResolver
        val uri =
            resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: throw Exception("Failed to create media store entry")

        resolver.openOutputStream(uri)?.use { outputStream ->
            outputStream.write(data)
        } ?: throw Exception("Failed to open output stream")
    }

    @Suppress("DEPRECATION")
    private fun saveToLegacyStorage(
        filename: String,
        data: ByteArray,
    ) {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs()
        }

        val file = File(downloadsDir, filename)
        FileOutputStream(file).use { it.write(data) }

        android.media.MediaScannerConnection.scanFile(
            context,
            arrayOf(file.absolutePath),
            null,
            null,
        )
    }

    private fun generateFilename(mimeType: String): String {
        val extension = extensionMap[mimeType] ?: "bin"
        return "download_${UUID.randomUUID().toString().take(ID_LEN)}.$extension"
    }
}
