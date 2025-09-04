package com.testlabs.browser

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.rule.ActivityTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Simple instrumentation test verifying activity launch.
 */
@RunWith(AndroidJUnit4::class)
@MediumTest
class SmokeTest {
    @Rule
    @JvmField
    val rule = ActivityTestRule(MainActivity::class.java)

    @Test
    fun launch() {
        rule.activity
    }
}
