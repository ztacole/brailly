plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

android {
    namespace = "com.brailly.friendly"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.brailly.friendly"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
        debug {
            ndk {
                // Tanpa pembatasan ini keempat ABI ikut dibundel, dan karena
                // minSdk 26 memakai extractNativeLibs=false, pustaka nativenya
                // disimpan tanpa kompresi sehingga ukuran APK berlipat.
                //
                // x86_64 wajib disertakan untuk emulator Intel. Bila hanya
                // arm64-v8a yang tersedia, emulator menjalankannya lewat
                // terjemahan biner, dan pustaka TFLite yang memakai instruksi
                // NEON dapat jatuh dengan SIGSEGV saat memeriksa kemampuan CPU.
                abiFilters += listOf("arm64-v8a", "x86_64")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    androidResources {
        // Model dipetakan langsung dari aset memakai openFd()+FileChannel.map,
        // sehingga berkasnya tidak boleh dikompresi di dalam APK.
        noCompress += "tflite"
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

room {
    // Lokasi skema basis data yang diekspor Room (berguna untuk migrasi & review).
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    // LiteRT
    implementation(libs.litert)

    // CameraX
    implementation(libs.androidx.camerax.core)
    implementation(libs.androidx.camerax.camera2)
    implementation(libs.androidx.camerax.lifecycle)
    implementation(libs.androidx.camerax.view)

    // Nav3
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.navigation3.lifecycle.viewmodel)

    // Kotlinx Serialization
    implementation(libs.kotlinx.serialization.core)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.paging)
    ksp(libs.androidx.room.compiler)

    // Paging 3 untuk riwayat pemindaian: daftarnya bertambah setiap kali
    // pengguna memindai, jadi dimuat sepotong-sepotong daripada sekaligus.
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)

    // YouTube IFrame Player untuk node video pembelajaran di Jalur Kuis.
    implementation(libs.youtube.player.core)

    // Coil untuk memuat thumbnail YouTube pada banner beranda.
    implementation(libs.coil.compose)

    // Koin
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
    implementation(libs.koin.compose.viewmodel)
    implementation(libs.koin.compose.navigation3)

    implementation(libs.androidx.core.splashscreen)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    testImplementation(libs.json)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}