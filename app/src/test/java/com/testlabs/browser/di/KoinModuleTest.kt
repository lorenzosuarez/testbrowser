package com.testlabs.browser.di

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
            modules(coreModule, settingsModule, browserModule)
        }
        stopKoin()
    }
}
