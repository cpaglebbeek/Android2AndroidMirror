// SPDX-License-Identifier: GPL-3.0-or-later OR Apache-2.0
//
// Vendored libadb-android 3.1.1 (github.com/MuntashirAkon/libadb-android).
// Lokaal opgenomen i.p.v. de JitPack-dependency (beslispunt 2b) zodat we naast het
// TCP-transport ook een USB-ADB-host kunnen injecteren: de upstream `AdbConnection`
// maakt zijn Socket in een private constructor zonder injectie-seam, dus een USB-transport
// vereist een lokale toevoeging aan deze module. Upstream-bron blijft ongewijzigd; onze
// additie(s) staan in een eigen constructor/klasse en zijn als zodanig gemarkeerd.
plugins {
    id("com.android.library")
}

android {
    namespace = "io.github.muntashirakon.adb"
    compileSdk = 35

    defaultConfig {
        // Upstream draait vanaf minSdk 1; wij pinnen op de app-ondergrens (26).
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation("androidx.annotation:annotation:1.9.1")
    // BouncyCastle: door libadb intern gebruikt (ADB-crypto). Zie crypto-layer-peel —
    // identiek aan de upstream-dep, dus geen nieuw runtime-gedrag t.o.v. de JitPack-build.
    implementation("org.bouncycastle:bcprov-jdk15to18:1.81")
    // SPAKE2 (libspake2.so) voor de Android 11+ draadloze pairing.
    implementation("com.github.MuntashirAkon.spake2-java:spake2-android:2.2.1")
}
