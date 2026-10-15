package com.brailly.friendly.di

import com.brailly.friendly.core.braille.BrailleTranslator
import com.brailly.friendly.data.scan.BrailleDetector
import com.brailly.friendly.ui.screen.learn.LearnViewModel
import com.brailly.friendly.ui.screen.scan.ScanViewModel
import com.brailly.friendly.ui.screen.translate.TranslateViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {

    // Core
    singleOf(::BrailleTranslator)

    // Networking (Ktor + JSON)
    single {
        HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
            install(Logging)
        }
    }

    // Data
    // Catatan: perapian teks dengan Gemma/Gemini tidak lagi dipakai pada alur
    // scan. Kelas-kelas di paket data.gemma dibiarkan ada namun tidak lagi
    // didaftarkan, sehingga tidak pernah dibuat.
    single { BrailleDetector(context = androidContext(), translator = get()) }

    // ViewModels
    viewModelOf(::TranslateViewModel)
    viewModelOf(::LearnViewModel)
    viewModelOf(::ScanViewModel)
}
