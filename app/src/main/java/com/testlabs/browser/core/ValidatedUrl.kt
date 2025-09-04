package com.testlabs.browser.core

/**
 * A validated URL value class that ensures proper URL formatting with HTTPS prefix.
 */
@JvmInline
public value class ValidatedUrl private constructor(public val value: String) {

    public companion object {
        private const val HTTPS_PREFIX = "https://"
        private const val HTTP_PREFIX = "http://"

        /**
         * Creates a ValidatedUrl from raw input, automatically adding HTTPS prefix if needed.
         */
        public fun fromInput(input: String): ValidatedUrl {
            val trimmed = input.trim()
            return when {
                trimmed.startsWith(HTTP_PREFIX) || trimmed.startsWith(HTTPS_PREFIX) -> ValidatedUrl(trimmed)
                trimmed.isBlank() -> ValidatedUrl("about:blank")
                else -> ValidatedUrl("$HTTPS_PREFIX$trimmed")
            }
        }

        /**
         * Creates a ValidatedUrl from a known valid URL string.
         */
        public fun fromValidUrl(url: String): ValidatedUrl = ValidatedUrl(url)
    }
}
