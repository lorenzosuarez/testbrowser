/**
 * Author: Lorenzo Suarez
 * Date: 09/06/2025
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

/**
 * Singleton holder for the app-wide CronetEngine.
 * Provides a shared, properly configured Cronet instance for all network requests.
 */
public object CronetHolder {
    @Volatile
    private var _engine: CronetEngine? = null

    private val executor = Executors.newCachedThreadPool { r ->
        Thread(r, "CronetExecutor").apply {
            isDaemon = true
        }
    }

    /**
     * Gets the shared CronetEngine instance, creating it if necessary.
     * This method is thread-safe and will return the same instance for all callers.
     */
    public fun getEngine(context: Context, userAgent: String): CronetEngine? {
        return _engine ?: synchronized(this) {
            _engine ?: createEngine(context, userAgent).also { _engine = it }
        }
    }

    /**
     * Force recreates the CronetEngine with new configuration.
     * Useful when user agent or other settings change.
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
            return CronetEngine.Builder(context)
                .enableHttp2(true)
                .enableQuic(true)  
                .enableBrotli(true)
                .apply {
                    
                    try {
                        
                        val enableZstdMethod = this::class.java.getDeclaredMethod("enableZstd", Boolean::class.javaPrimitiveType)
                        enableZstdMethod.invoke(this, true)
                        Log.d(TAG, "ZSTD compression enabled successfully")
                    } catch (e: NoSuchMethodException) {
                        Log.d(TAG, "ZSTD compression not available in this Cronet version (method not found) - error: ${e.message}")
                    } catch (e: Exception) {
                        Log.d(TAG, "ZSTD compression not available: ${e.javaClass.simpleName}")
                    }
                }
                .enableHttpCache(CronetEngine.Builder.HTTP_CACHE_IN_MEMORY, 20L * 1024 * 1024) 
                .setUserAgent(userAgent)
                .setThreadPriority(Thread.NORM_PRIORITY)
        }

        return try {
            
            Log.d(TAG, "Creating Cronet engine with UA: ${userAgent.take(100)}")
            createBuilder().build().also {
                Log.d(TAG, "Cronet engine created successfully")
            }
        } catch (e: Exception) {
            Log.d(TAG, "Direct Cronet creation failed, trying with provider installation: ${e.message}")

            try {
                
                val installTask = CronetProviderInstaller.installProvider(context)
                Tasks.await(installTask, 5, TimeUnit.SECONDS)

                Log.d(TAG, "Cronet provider installed successfully")
                createBuilder().build().also {
                    Log.d(TAG, "Cronet engine created successfully after provider installation")
                }
            } catch (providerException: Exception) {
                Log.w(TAG, "Failed to install Cronet provider or create engine", providerException)
                null
            }
        }
    }

    /**
     * Gets the shared executor for Cronet operations.
     */
    public fun getExecutor(): ExecutorService? = executor

    /**
     * Shuts down the Cronet engine and executor when the app is destroyed.
     * Should be called from Application.onTerminate() or similar.
     */
    public fun shutdown() {
        synchronized(this) {
            _engine?.shutdown()
            _engine = null
            executor.shutdown()
        }
    }
}
