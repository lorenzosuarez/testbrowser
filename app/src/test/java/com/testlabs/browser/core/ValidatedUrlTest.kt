/**
 * Author: Lorenzo Suarez
 * Date: 09/06/2025
 */


package com.testlabs.browser.core

import org.junit.Test
import kotlin.test.assertEquals

/**
 * Unit tests for ValidatedUrl value class.
 */
class ValidatedUrlTest {
    @Test
    fun `fromInput adds https prefix to plain domain`() {
        val result = ValidatedUrl.fromInput("example.com")
        assertEquals("https://example.com", result.value)
    }

    @Test
    fun `fromInput preserves existing https prefix`() {
        val input = "https://example.com"
        val result = ValidatedUrl.fromInput(input)
        assertEquals(input, result.value)
    }

    @Test
    fun `fromInput preserves existing http prefix`() {
        val input = "http://example.com"
        val result = ValidatedUrl.fromInput(input)
        assertEquals(input, result.value)
    }

    @Test
    fun `fromInput handles blank input with about blank`() {
        val result = ValidatedUrl.fromInput("")
        assertEquals("about:blank", result.value)
    }

    @Test
    fun `fromInput trims whitespace`() {
        val result = ValidatedUrl.fromInput("  example.com  ")
        assertEquals("https://example.com", result.value)
    }
}
