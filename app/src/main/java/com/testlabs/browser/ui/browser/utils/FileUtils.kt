/**
 * Author: Lorenzo Suarez
 * Date: 09/08/2025
 */
package com.testlabs.browser.ui.browser.utils

import android.webkit.MimeTypeMap

/**
 * Utilities for handling file operations and metadata extraction.
 */
public object FileUtils {

    /**
     * Extracts filename from content disposition header or URL with MIME type fallback.
     */
    public fun extractFileName(contentDisposition: String?, url: String, mimeType: String?): String {
        var fileName: String? = null
        if (contentDisposition != null) {
            val index = contentDisposition.indexOf("filename=")
            if (index >= 0) {
                fileName = contentDisposition.substring(index + 9)
                fileName = fileName.replace("\"", "")
            }
        }
        if (fileName == null) {
            fileName = url.substringAfterLast("/")
            if (fileName.isEmpty()) {
                fileName = "download"
            }
        }
        if (!fileName.contains('.') && mimeType != null) {
            val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
            if (extension != null) {
                fileName += ".$extension"
            }
        }
        return fileName
    }
}
