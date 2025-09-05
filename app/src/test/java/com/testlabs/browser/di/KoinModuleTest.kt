/*
 * @author Lorenzo Suarez
 * @date 09/04//2025
 */

package com.testlabs.browser.di

import android.content.Context
import io.mockk.mockk
import org.junit.Test
import org.koin.core.context.stopKoin
import org.koin.test.AutoCloseKoinTest
import org.koin.test.check.checkModules

/**
 * Verifies Koin modules configuration.
 */
class KoinModuleTest : AutoCloseKoinTest() {
    @Test
    fun modulesLoad() {
        checkModules {
            withInstance<Context>(mockk(relaxed = true))
            modules(appModule, settingsModule, coreModule)
        }
        stopKoin()
    }
}
