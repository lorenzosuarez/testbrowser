/**
 * Author: Lorenzo Suarez
 * Date: 06/09/2025
 */

package com.testlabs.browser.ui.browser

/**
 * Provides version information required to construct User-Agent strings.
 */
public interface VersionProvider {
    /** Android OS version string, e.g. "14". */
    public fun androidVersion(): String

    /** Full Chrome version string, e.g. "123.0.6312.99". */
    public fun chromeFullVersion(): String

    /** Major Chrome version number extracted from [chromeFullVersion]. */
    public fun chromeMajor(): Int

    /** Device model reported in the UA string. */
    public fun deviceModel(): String

    /** Chrome major version as a string for convenience. */
    public fun major(): String = chromeMajor().toString()
}
