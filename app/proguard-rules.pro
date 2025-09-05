# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Keep WebView related classes
-keep class android.webkit.** { *; }
-keep class com.android.webview.** { *; }
-keep class androidx.webkit.** { *; }

# Keep Koin related classes
-keep class org.koin.** { *; }
-keep interface org.koin.** { *; }

# Keep Coil related classes for image loading
-keep class coil.** { *; }
-dontwarn coil.**

# Keep model classes and value classes
-keep class com.testlabs.browser.core.** { *; }
-keep class com.testlabs.browser.ui.browser.BrowserState { *; }
-keep class com.testlabs.browser.ui.browser.BrowserIntent* { *; }
-keep class com.testlabs.browser.ui.browser.BrowserEffect* { *; }

# Keep compose runtime classes
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Brotli decoder
-keep class org.brotli.dec.** { *; }

# Zstandard decoder
-keep class com.github.luben.zstd.** { *; }
