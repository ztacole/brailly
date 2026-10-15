package com.brailly.friendly.di

import com.brailly.friendly.core.braille.BrailleTranslator
import com.brailly.friendly.data.gemma.GemmaClient
import com.brailly.friendly.data.gemma.GemmaModelDownloader
import com.brailly.friendly.data.gemma.OnDeviceGemma
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
    single { OnDeviceGemma(context = androidContext()) }
    single { GemmaModelDownloader(context = androidContext(), http = get()) }
    single { GemmaClient(http = get(), onDevice = get()) }
    single { BrailleDetector(context = androidContext(), translator = get()) }

    // ViewModels
    viewModelOf(::TranslateViewModel)
    viewModelOf(::LearnViewModel)
    viewModelOf(::ScanViewModel)
}
