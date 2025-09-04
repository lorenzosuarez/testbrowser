package com.testlabs.browser

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.testlabs.browser.ui.browser.BrowserScreen
import com.testlabs.browser.ui.browser.UAProvider
import com.testlabs.browser.ui.theme.TestBrowserTheme
import org.koin.android.ext.android.inject

/**
 * Single activity hosting the browser interface with proper system UI handling.
 * Enables edge-to-edge display and ensures proper theme support.
 */
public class MainActivity : ComponentActivity() {

    private val uaProvider: UAProvider by inject()

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // File picker result is handled by WebView's FileChooserParams callback
        // This launcher is just to trigger the system file picker
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            TestBrowserTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BrowserScreen(
                        filePickerLauncher = filePickerLauncher,
                        uaProvider = uaProvider
                    )
                }
            }
        }
    }
}
