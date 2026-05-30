// ── app/build.gradle.kts ──────────────────────────────────────

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)   // ← already includes kotlin.android internally
    alias(libs.plugins.ksp)
    // ✖ DO NOT add libs.plugins.kotlin.android — it conflicts with kotlin.compose on AGP 9+
}

android {
    namespace = "com.example.rasaushadhies"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.rasaushadhies"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // AGP 9+ uses jvmToolchain instead of kotlinOptions { jvmTarget }
    //noinspection WrongGradleMethod
    kotlin {
        jvmToolchain(17)
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    // ── Compose BOM ───────────────────────────────────────────
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("org.json:json:20231013")
    // ── Compose UI ────────────────────────────────────────────
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    // ── Material 3 ────────────────────────────────────────────
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.google.material)

    // ── Core ──────────────────────────────────────────────────
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)

    // ── Activity ──────────────────────────────────────────────
    implementation(libs.androidx.activity.compose)

    // ── Lifecycle ─────────────────────────────────────────────
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // ── Navigation ────────────────────────────────────────────
    implementation(libs.androidx.navigation.compose)

    // ── Room ──────────────────────────────────────────────────
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // ── Retrofit + OkHttp ─────────────────────────────────────
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)

    // ── Coroutines ────────────────────────────────────────────
    implementation(libs.kotlinx.coroutines.android)

    // ── DataStore ─────────────────────────────────────────────
    implementation(libs.androidx.datastore.preferences)

    // ── Testing ───────────────────────────────────────────────
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}