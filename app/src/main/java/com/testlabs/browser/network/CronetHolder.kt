/**
 * Author: Lorenzo Suarez
 * Date: 09/08/2025
 */
package com.testlabs.browser.network

import android.content.Context
import android.util.Log
import com.google.android.gms.net.CronetProviderInstaller
import com.google.android.gms.tasks.Tasks
import org.chromium.net.CronetEngine
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

private const val TAG = "CronetHolder"

public object CronetHolder {
    @Volatile
    private var _engine: CronetEngine? = null

    private val executor: ExecutorService = Executors.newCachedThreadPool { r: Runnable ->
        Thread(r, "CronetExecutor").apply { isDaemon = true }
    }

    /**
     * Returns the shared CronetEngine instance, creating it if necessary.
     */
    public fun getEngine(context: Context, userAgent: String): CronetEngine? {
        return _engine ?: synchronized(this) {
            _engine ?: createEngine(context, userAgent).also { _engine = it }
        }
    }

    /**
     * Recreates the CronetEngine with a new configuration.
     */
    public fun recreateEngine(context: Context, userAgent: String): CronetEngine? {
        synchronized(this) {
            _engine?.shutdown()
            _engine = null
            return createEngine(context, userAgent).also { _engine = it }
        }
    }

    private fun createEngine(context: Context, userAgent: String): CronetEngine? {
        fun createBuilder(): CronetEngine.Builder {
            val builder = CronetEngine.Builder(context)
                .enableHttp2(true)
                .enableQuic(false)

            try {
                CronetEngine.Builder::class.java
                    .getMethod("enableBrotli", Boolean::class.javaPrimitiveType)
                    .invoke(builder, true)
            } catch (_: Exception) {
                Log.d(TAG, "Brotli not available")
            }

            val experimental = """
                {
                  "disable_certificate_compression": false,
                  "enable_certificate_compression_brotli": true,
                  "enable_encrypted_client_hello": false,
                  "enable_tls13_early_data": false,
                  "enable_tls13_kyber": false
                }
            """.trimIndent()
            try {
                CronetEngine.Builder::class.java
                    .getMethod("setExperimentalOptions", String::class.java)
                    .invoke(builder, experimental)
            } catch (_: Exception) {
                Log.d(TAG, "Experimental options not supported")
            }

            builder.setUserAgent(userAgent)
            return builder
        }

        return try {
            Log.d(TAG, "Creating Cronet engine")
            createBuilder().build().also {
                Log.d(TAG, "Cronet engine created")
            }
        } catch (e: Exception) {
            Log.d(TAG, "Direct Cronet creation failed, trying provider install: ${e.message}")
            try {
                val installTask = CronetProviderInstaller.installProvider(context)
                Tasks.await(installTask, 5, TimeUnit.SECONDS)
                Log.d(TAG, "Cronet provider installed")
                createBuilder().build().also {
                    Log.d(TAG, "Cronet engine created after provider install")
                }
            } catch (providerException: Exception) {
                Log.w(TAG, "Failed to install Cronet provider or create engine", providerException)
                null
            }
        }
    }

    /**
     * Returns the shared executor for Cronet operations.
     */
    public fun getExecutor(): ExecutorService = executor

    /**
     * Shuts down the Cronet engine and executor.
     */
    public fun shutdown() {
        synchronized(this) {
            _engine?.shutdown()
            _engine = null
            executor.shutdown()
        }
    }
}
