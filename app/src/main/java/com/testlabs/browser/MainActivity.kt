/**
 * Author: Lorenzo Suarez
 * Date: 09/08/2025
 */

package com.testlabs.browser

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import com.testlabs.browser.ui.browser.BrowserScreen
import com.testlabs.browser.ui.browser.UAProvider
import com.testlabs.browser.ui.theme.TestBrowserTheme
import org.koin.android.ext.android.inject

public class MainActivity : ComponentActivity() {

    private val uaProvider: UAProvider by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TestBrowserTheme {
                BrowserApp()
            }
        }
    }

    @Composable
    private fun BrowserApp() {
        val filePickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) { result ->
            
        }

        BrowserScreen(
            filePickerLauncher = filePickerLauncher,
            uaProvider = uaProvider
        )
    }
}
