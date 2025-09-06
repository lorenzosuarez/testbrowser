package com.testlabs.browser.ui.browser

import android.os.Build
import com.testlabs.browser.network.UserAgentProvider

/**
 * G. JS-visible parity at document_start
 * Injects Chrome Mobile navigator properties before any page script runs
 */
public class ChromeCompatibilityInjector(
    private val userAgentProvider: UserAgentProvider
) {

    public fun generateChromeCompatibilityScript(): String {
        val brands = userAgentProvider.getChromeUserAgentDataBrands()
        val platformInfo = userAgentProvider.getPlatformInfo()
        val chromeUA = userAgentProvider.getChromeStableMobileUA()

        return """
            (function() {
                'use strict';
                
                // G. JS-visible parity - navigator.userAgent override
                try {
                    Object.defineProperty(navigator, 'userAgent', {
                        value: '$chromeUA',
                        writable: false,
                        enumerable: true,
                        configurable: true
                    });
                } catch (e) {
                    console.log('Could not override navigator.userAgent:', e.message);
                }
                
                // G. JS-visible parity - navigator.language and languages
                try {
                    Object.defineProperty(navigator, 'language', {
                        value: 'en-US',
                        writable: false,
                        enumerable: true,
                        configurable: true
                    });
                    
                    Object.defineProperty(navigator, 'languages', {
                        value: ['en-US', 'en'],
                        writable: false,
                        enumerable: true,
                        configurable: true
                    });
                } catch (e) {
                    console.log('Could not override navigator.language:', e.message);
                }
                
                // G. JS-visible parity - navigator.platform
                try {
                    Object.defineProperty(navigator, 'platform', {
                        value: '${platformInfo["platform"]}',
                        writable: false,
                        enumerable: true,
                        configurable: true
                    });
                } catch (e) {
                    console.log('Could not override navigator.platform:', e.message);
                }
                
                // G. JS-visible parity - navigator.vendor
                try {
                    Object.defineProperty(navigator, 'vendor', {
                        value: 'Google Inc.',
                        writable: false,
                        enumerable: true,
                        configurable: true
                    });
                } catch (e) {
                    console.log('Could not override navigator.vendor:', e.message);
                }
                
                // G. JS-visible parity - window.chrome object
                try {
                    if (typeof window.chrome === 'undefined') {
                        window.chrome = {
                            app: {
                                isInstalled: false,
                                InstallState: {
                                    DISABLED: 'disabled',
                                    INSTALLED: 'installed',
                                    NOT_INSTALLED: 'not_installed'
                                },
                                RunningState: {
                                    CANNOT_RUN: 'cannot_run',
                                    READY_TO_RUN: 'ready_to_run',
                                    RUNNING: 'running'
                                }
                            },
                            runtime: {
                                onConnect: null,
                                onMessage: null
                            }
                        };
                    }
                } catch (e) {
                    console.log('Could not create window.chrome:', e.message);
                }
                
                // G. JS-visible parity - navigator.userAgentData
                try {
                    const brands = [
                        ${brands.map { """{ brand: "${it.first}", version: "${it.second}" }""" }.joinToString(", ")}
                    ];
                    
                    const userAgentData = {
                        brands: brands,
                        mobile: ${platformInfo["mobile"]},
                        platform: 'Android',
                        
                        getHighEntropyValues: function(hints) {
                            return Promise.resolve({
                                architecture: '${platformInfo["architecture"]}',
                                bitness: '${platformInfo["bitness"]}',
                                brands: brands,
                                fullVersionList: brands,
                                mobile: ${platformInfo["mobile"]},
                                model: '${platformInfo["model"]}',
                                platform: 'Android',
                                platformVersion: '${platformInfo["platformVersion"]}',
                                uaFullVersion: '${userAgentProvider.getChromeStableFullVersion()}'
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
                } catch (e) {
                    console.log('Could not create navigator.userAgentData:', e.message);
                }
                
                console.log('Chrome compatibility injection completed');
            })();
        """.trimIndent()
    }
}
