package com.testlabs.browser

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.testlabs.browser.ui.browser.BrowserScreen
import com.testlabs.browser.ui.browser.UAProvider
import org.koin.android.ext.android.inject

public class MainActivity : ComponentActivity() {

    private val uaProvider: UAProvider by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                BrowserApp()
            }
        }
    }

    @Composable
    private fun BrowserApp() {
        val filePickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) { result ->
            // Handle file picker result if needed
        }

        BrowserScreen(
            filePickerLauncher = filePickerLauncher,
            uaProvider = uaProvider
        )
    }
}
