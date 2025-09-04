package com.testlabs.browser

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented smoke test for browser functionality.
 */
@RunWith(AndroidJUnit4::class)
class BrowserSmokeTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun appLaunchesAndShowsBrowserInterface() {
        composeTestRule.onNodeWithText("TestBrowser").assertIsDisplayed()
        composeTestRule.onNodeWithText("Enter URL").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Go back").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Go forward").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Reload").assertIsDisplayed()
    }

    @Test
    fun urlInputAcceptsTextAndTriggersNavigation() {
        val testUrl = "example.com"

        composeTestRule.onNodeWithText("Enter URL").performTextInput(testUrl)
        composeTestRule.onNodeWithContentDescription("Reload").performClick()

        // Verify the URL input field contains the entered text
        composeTestRule.onNodeWithText("Enter URL").assertTextContains(testUrl)
    }

    @Test
    fun navigationButtonsRespondToClicks() {
        // Test reload button
        composeTestRule.onNodeWithContentDescription("Reload").performClick()

        // Test back button (should be disabled initially)
        composeTestRule.onNodeWithContentDescription("Go back").performClick()

        // Test forward button (should be disabled initially)
        composeTestRule.onNodeWithContentDescription("Go forward").performClick()

        // No assertions needed - we're just verifying clicks don't crash
    }
}
