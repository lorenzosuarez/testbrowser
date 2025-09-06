package com.testlabs.browser.ui.browser

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.activity.result.ActivityResultLauncher

private const val TAG = "FileUploadHandler"

/**
 * Handles file upload chooser for WebView
 */
public class FileUploadHandler(private val context: Context) {

    private var filePickerLauncher: ActivityResultLauncher<Intent>? = null
    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    public fun initialize(launcher: ActivityResultLauncher<Intent>) {
        this.filePickerLauncher = launcher
    }

    public fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: WebChromeClient.FileChooserParams?
    ): Boolean {
        Log.d(TAG, "File chooser requested")

        // Store the callback for later use
        this.filePathCallback = filePathCallback

        try {
            val intent = fileChooserParams?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }

            filePickerLauncher?.launch(intent)
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error launching file chooser", e)
            filePathCallback?.onReceiveValue(null)
            return false
        }
    }
}
