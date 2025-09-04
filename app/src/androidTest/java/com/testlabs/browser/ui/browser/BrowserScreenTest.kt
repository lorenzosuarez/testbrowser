package com.testlabs.browser.ui.browser

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for BrowserScreen composable.
 */
@RunWith(AndroidJUnit4::class)
class BrowserScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun browserControlsDisplayCorrectInitialState() {
        composeTestRule.setContent {
            BrowserControls(
                urlInput = "",
                onUrlInputChanged = {},
                onUrlSubmitted = {},
                canGoBack = false,
                canGoForward = false,
                onBackClicked = {},
                onForwardClicked = {},
                onReloadClicked = {}
            )
        }

        composeTestRule.onNodeWithText("Enter URL").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Go back").assertIsNotEnabled()
        composeTestRule.onNodeWithContentDescription("Go forward").assertIsNotEnabled()
        composeTestRule.onNodeWithContentDescription("Reload").assertIsEnabled()
    }

    @Test
    fun navigationButtonsEnabledWhenNavigationPossible() {
        composeTestRule.setContent {
            BrowserControls(
                urlInput = "https://example.com",
                onUrlInputChanged = {},
                onUrlSubmitted = {},
                canGoBack = true,
                canGoForward = true,
                onBackClicked = {},
                onForwardClicked = {},
                onReloadClicked = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("Go back").assertIsEnabled()
        composeTestRule.onNodeWithContentDescription("Go forward").assertIsEnabled()
        composeTestRule.onNodeWithContentDescription("Reload").assertIsEnabled()
    }
}
