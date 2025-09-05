plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.testlabs.browser"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.testlabs.browser"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
        freeCompilerArgs +=
            listOf(
                "-opt-in=kotlin.RequiresOptIn",
                "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
                "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
                "-Xjsr305=strict",
                "-Xjvm-default=all",
            )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.kotlin.get()
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

kotlin {
    explicitApi()
}

dependencies {
    implementation(libs.androidx.core.ktx.v1170)
    implementation(libs.androidx.lifecycle.runtime.ktx.v293)
    implementation(libs.androidx.lifecycle.viewmodel.compose.v293)
    implementation(libs.androidx.webkit)
    implementation(libs.androidx.startup.runtime)
    implementation(platform(libs.compose.bom.v20250801))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.activity.compose.v1101)
    implementation(libs.androidx.navigation.compose.v293)
    implementation(libs.androidx.animation)
    implementation(libs.androidx.material)
    implementation(libs.accompanist.swiperefresh)
    implementation(libs.kotlinx.coroutines.android.v1102)
    implementation(libs.datastore)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.koin.android.v356)
    implementation(libs.koin.androidx.compose.v356)
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)
    implementation(libs.coil.compose)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test.v1102)
    testImplementation(libs.turbine.v121)
    testImplementation(libs.mockk.v1145)
    testImplementation(libs.koin.test.v356)
    androidTestImplementation(libs.androidx.junit.v130)
    androidTestImplementation(libs.androidx.espresso.core.v370)
    androidTestImplementation(libs.androidx.core.v170)
    androidTestImplementation(platform(libs.compose.bom.v20250801))
    androidTestImplementation(libs.androidx.ui.test.junit4)
}

detekt {
    config.setFrom("$projectDir/../config/detekt.yml")
    buildUponDefaultConfig = true
}

ktlint {
    debug = true
    verbose = true
    android = true
    outputColorName = "RED"
}
