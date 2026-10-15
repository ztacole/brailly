package com.brailly.friendly.di

import com.brailly.friendly.core.braille.BrailleTranslator
import com.brailly.friendly.data.scan.BrailleDetector
import com.brailly.friendly.ui.screen.learn.LearnViewModel
import com.brailly.friendly.ui.screen.scan.ScanViewModel
import com.brailly.friendly.ui.screen.translate.TranslateViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {

    // Core
    singleOf(::BrailleTranslator)

    // Data
    // Pemindaian berjalan sepenuhnya di perangkat memakai brailly.tflite,
    // tanpa jaringan dan tanpa perapian oleh model bahasa. Karena itu tidak
    // ada HttpClient yang perlu didaftarkan di sini.
    single { BrailleDetector(context = androidContext(), translator = get()) }

    // ViewModels
    viewModelOf(::TranslateViewModel)
    viewModelOf(::LearnViewModel)
    viewModelOf(::ScanViewModel)
}
