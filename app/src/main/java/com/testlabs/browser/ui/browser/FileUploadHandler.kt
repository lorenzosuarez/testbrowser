/*
 * @author Lorenzo Suarez
 * @date 09/04//2025
 */

package com.testlabs.browser.ui.browser

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import androidx.activity.result.ActivityResultLauncher

/**
 * WebView file upload orchestration with MIME filtering and safe callback dispatch.
 */
public class FileUploadHandler(
    private val context: Context,
) {
    private companion object {
        private const val TITLE = "Select File"
        private const val ALL = "*/*"
        private const val IMAGE_PREFIX = "image/"
    }

    private var callback: ValueCallback<Array<Uri>>? = null
    private var launcher: ActivityResultLauncher<Intent>? = null

    /**
     * Initialize the upload handler with the activity result launcher.
     */
    public fun initialize(resultLauncher: ActivityResultLauncher<Intent>) {
        launcher = resultLauncher
    }

    /**
     * Handle file chooser request from WebView.
     */
    public fun handleFileChooser(
        cb: ValueCallback<Array<Uri>>?,
        params: WebChromeClient.FileChooserParams?,
    ): Boolean {
        cancel()
        callback = cb
        val l = launcher ?: return false.also { cancel() }
        return runCatching {
            l.launch(buildChooserIntent(params))
            true
        }.getOrElse {
            cancel()
            false
        }
    }

    /**
     * Handle the result from file picker activity.
     */
    public fun handleActivityResult(data: Intent?) {
        runCatching { callback?.onReceiveValue(extract(data)) }
            .onFailure { callback?.onReceiveValue(null) }
        callback = null
    }

    /**
     * Cancel any pending upload operation.
     */
    public fun cancel() {
        callback?.onReceiveValue(null)
        callback = null
    }

    /**
     * Check if there's a pending upload operation.
     */
    public val hasPending: Boolean get() = callback != null

    private fun buildChooserIntent(params: WebChromeClient.FileChooserParams?): Intent {
        val base =
            Intent(Intent.ACTION_GET_CONTENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = ALL
                if (params?.mode == WebChromeClient.FileChooserParams.MODE_OPEN_MULTIPLE) {
                    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                }
                configureMimeTypes(params)
            }
        return Intent.createChooser(base, TITLE).apply { maybeAddCamera(params) }
    }

    private fun Intent.configureMimeTypes(params: WebChromeClient.FileChooserParams?) {
        val types = params?.acceptTypes.orEmpty().filter { it.isNotBlank() && it != ALL }
        if (types.isNotEmpty()) {
            type = types.first()
            if (types.size > 1) putExtra(Intent.EXTRA_MIME_TYPES, types.drop(1).toTypedArray())
        }
    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun Intent.maybeAddCamera(params: WebChromeClient.FileChooserParams?) {
        val acceptImages = params?.acceptTypes?.any { it.startsWith(IMAGE_PREFIX) } == true
        if (!acceptImages) return
        val camera = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (camera.resolveActivity(context.packageManager) != null) {
            putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(camera))
        }
    }

    private fun extract(data: Intent?): Array<Uri>? {
        return when {
            data == null -> null
            data.data != null -> arrayOf(data.data ?: return null)
            data.clipData != null -> {
                Array(data.clipData?.itemCount ?: 0) { i ->
                    data.clipData?.getItemAt(i)?.uri
                }.filterNotNull().toTypedArray()
            }
            else -> null
        }
    }
}
