plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "nl.icthorse.android2androidmirror"
    compileSdk = 35

    defaultConfig {
        applicationId = "nl.icthorse.android2androidmirror"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "0.0.2-Torres"
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // In-app ADB-client (beslispunt 2, herzien) — libadb-android: echte Android 11+
    // draadloze pairing (SPAKE2 + TLS) + connect + shell + sync, no-root.
    // Dual-licensed GPL-3.0-or-later OR Apache-2.0 → AGPL-3.0-compatibel.
    // VENDORED als lokale :libadb-module (beslispunt 2b) om een USB-ADB-host te injecteren.
    implementation(project(":libadb"))
    // X509-certgeneratie voor het ADB-TLS-clientcertificaat (sun.security.x509 backport).
    implementation("com.github.MuntashirAkon:sun-security-android:1.1")
    // Custom Conscrypt: aanbevolen om met een REMOTE adbd te TLS-verbinden (libadb README).
    implementation("org.conscrypt:conscrypt-android:2.5.3")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
