/**
 * Author: Lorenzo Suarez
 * Date: 09/06/2025
 */
package com.testlabs.browser.ui.browser

/**
 * High-quality Chrome compatibility injector that creates JavaScript to make WebView
 * appear as Chrome Mobile to websites and fingerprinting scripts.
 *
 * This injector provides comprehensive navigator property overrides including:
 * - navigator.userAgent matching Chrome Mobile
 * - navigator.userAgentData with realistic Chrome brand information
 * - navigator.platform, vendor, language properties
 * - window.chrome object with proper structure
 * - Screen and hardware properties matching real Chrome Mobile
 *
 * The generated script runs at document_start to ensure it executes before any page scripts.
 */
public class ChromeCompatibilityInjector(
    private val uaProvider: UAProvider
) {

    /**
     * Generates a comprehensive Chrome compatibility script that overrides navigator properties
     * and creates Chrome-specific objects to mimic a real Chrome Mobile browser.
     *
     * @param desktopMode Whether to use desktop or mobile Chrome appearance
     * @return JavaScript code that should be injected at document_start
     */
    public fun generateChromeCompatibilityScript(desktopMode: Boolean = false): String {
        val userAgent = uaProvider.userAgent(desktop = desktopMode)
        val chromeVersion = extractChromeVersion(userAgent)
        val platform = if (desktopMode) "Win32" else "Linux armv8l"
        val brands = generateUserAgentDataBrands(chromeVersion)

        return """
            (function() {
                'use strict';
                
                console.log('Chrome compatibility injection starting...');
                
                // Override navigator.userAgent to match Chrome Mobile
                try {
                    Object.defineProperty(navigator, 'userAgent', {
                        value: '$userAgent',
                        writable: false,
                        enumerable: true,
                        configurable: true
                    });
                    console.log('navigator.userAgent set to Chrome Mobile');
                } catch (e) {
                    console.warn('Could not override navigator.userAgent:', e.message);
                }
                
                // Override navigator.platform
                try {
                    Object.defineProperty(navigator, 'platform', {
                        value: '$platform',
                        writable: false,
                        enumerable: true,
                        configurable: true
                    });
                } catch (e) {
                    console.warn('Could not override navigator.platform:', e.message);
                }
                
                // Override navigator.vendor
                try {
                    Object.defineProperty(navigator, 'vendor', {
                        value: 'Google Inc.',
                        writable: false,
                        enumerable: true,
                        configurable: true
                    });
                } catch (e) {
                    console.warn('Could not override navigator.vendor:', e.message);
                }
                
                // Override navigator.language and languages
                try {
                    Object.defineProperty(navigator, 'language', {
                        value: 'en-US',
                        writable: false,
                        enumerable: true,
                        configurable: true
                    });
                    
                    Object.defineProperty(navigator, 'languages', {
                        value: Object.freeze(['en-US', 'en']),
                        writable: false,
                        enumerable: true,
                        configurable: true
                    });
                } catch (e) {
                    console.warn('Could not override navigator.language:', e.message);
                }
                
                // Create realistic navigator.userAgentData
                try {
                    const userAgentData = {
                        brands: Object.freeze($brands),
                        mobile: ${!desktopMode},
                        platform: '$platform',
                        getHighEntropyValues: function(hints) {
                            return Promise.resolve({
                                architecture: '${if (desktopMode) "x86" else "arm"}',
                                bitness: '${if (desktopMode) "64" else "64"}',
                                brands: this.brands,
                                fullVersionList: this.brands,
                                mobile: this.mobile,
                                model: '${if (desktopMode) "" else ""}',
                                platform: this.platform,
                                platformVersion: '${if (desktopMode) "10.0.0" else "6.0.1"}',
                                uaFullVersion: '$chromeVersion.0.0.0',
                                wow64: false
                            });
                        },
                        toJSON: function() {
                            return {
                                brands: this.brands,
                                mobile: this.mobile,
                                platform: this.platform
                            };
                        }
                    };
                    
                    Object.defineProperty(navigator, 'userAgentData', {
                        value: userAgentData,
                        writable: false,
                        enumerable: true,
                        configurable: true
                    });
                    console.log('navigator.userAgentData created');
                } catch (e) {
                    console.warn('Could not create navigator.userAgentData:', e.message);
                }
                
                // Create window.chrome object
                try {
                    if (typeof window.chrome === 'undefined') {
                        window.chrome = {
                            app: {
                                isInstalled: false,
                                InstallState: Object.freeze({
                                    DISABLED: 'disabled',
                                    INSTALLED: 'installed',
                                    NOT_INSTALLED: 'not_installed'
                                }),
                                RunningState: Object.freeze({
                                    CANNOT_RUN: 'cannot_run',
                                    READY_TO_RUN: 'ready_to_run',
                                    RUNNING: 'running'
                                })
                            },
                            runtime: {
                                onConnect: null,
                                onMessage: null
                            },
                            csi: function() { return {}; },
                            loadTimes: function() { 
                                return {
                                    requestTime: Date.now() / 1000,
                                    startLoadTime: Date.now() / 1000,
                                    commitLoadTime: Date.now() / 1000,
                                    finishDocumentLoadTime: Date.now() / 1000,
                                    finishLoadTime: Date.now() / 1000,
                                    firstPaintTime: Date.now() / 1000,
                                    firstPaintAfterLoadTime: 0,
                                    navigationType: 'Other',
                                    wasFetchedViaSpdy: false,
                                    wasNpnNegotiated: false,
                                    npnNegotiatedProtocol: 'unknown',
                                    wasAlternateProtocolAvailable: false,
                                    connectionInfo: 'unknown'
                                };
                            }
                        };
                        
                        Object.defineProperty(window, 'chrome', {
                            value: window.chrome,
                            writable: false,
                            enumerable: true,
                            configurable: true
                        });
                        console.log('window.chrome object created');
                    }
                } catch (e) {
                    console.warn('Could not create window.chrome:', e.message);
                }
                
                // Override navigator.webdriver
                try {
                    Object.defineProperty(navigator, 'webdriver', {
                        value: undefined,
                        writable: false,
                        enumerable: true,
                        configurable: true
                    });
                } catch (e) {
                    console.warn('Could not override navigator.webdriver:', e.message);
                }
                
                // Override screen properties for mobile consistency
                try {
                    if (!$desktopMode) {
                        const screenProps = {
                            availWidth: 412,
                            availHeight: 915,
                            width: 412,
                            height: 915,
                            colorDepth: 24,
                            pixelDepth: 24
                        };
                        
                        Object.keys(screenProps).forEach(prop => {
                            try {
                                Object.defineProperty(screen, prop, {
                                    value: screenProps[prop],
                                    writable: false,
                                    enumerable: true,
                                    configurable: true
                                });
                            } catch (e) {
                                console.warn('Could not override screen.' + prop + ':', e.message);
                            }
                        });
                    }
                } catch (e) {
                    console.warn('Could not override screen properties:', e.message);
                }
                
                // Override devicePixelRatio for mobile
                try {
                    if (!$desktopMode) {
                        Object.defineProperty(window, 'devicePixelRatio', {
                            value: 2.625,
                            writable: false,
                            enumerable: true,
                            configurable: true
                        });
                    }
                } catch (e) {
                    console.warn('Could not override devicePixelRatio:', e.message);
                }
                
                console.log('Chrome compatibility injection completed successfully');
            })();
        """.trimIndent()
    }

    /**
     * Extracts Chrome version from user agent string.
     * Falls back to a reasonable default if extraction fails.
     */
    private fun extractChromeVersion(userAgent: String): String {
        return runCatching {
            val chromeRegex = Regex("""Chrome/(\d+)""")
            chromeRegex.find(userAgent)?.groupValues?.get(1) ?: DEFAULT_CHROME_VERSION
        }.getOrElse { DEFAULT_CHROME_VERSION }
    }

    /**
     * Generates realistic Chrome brand information for navigator.userAgentData.
     * This includes the main Chrome brand plus additional entropy brands.
     */
    private fun generateUserAgentDataBrands(chromeVersion: String): String {
        val majorVersion = chromeVersion.toIntOrNull() ?: DEFAULT_CHROME_VERSION.toInt()

        
        val brands = listOf(
            mapOf("brand" to "Google Chrome", "version" to majorVersion.toString()),
            mapOf("brand" to "Chromium", "version" to majorVersion.toString()),
            mapOf("brand" to "Not=A?Brand", "version" to "24") 
        )

        return brands.joinToString(", ", "[", "]") { brand ->
            "{\"brand\": \"${brand["brand"]}\", \"version\": \"${brand["version"]}\"}"
        }
    }

    public companion object {
        private const val DEFAULT_CHROME_VERSION = "119"
    }
}
