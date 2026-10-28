package com.brailly.friendly.di

import com.brailly.friendly.core.braille.BrailleTranslator
import com.brailly.friendly.data.learn.BraillyDatabase
import com.brailly.friendly.data.learn.LearnProgressRepository
import com.brailly.friendly.data.scan.BrailleDetector
import com.brailly.friendly.data.scan.ScanHistoryRepository
import com.brailly.friendly.data.speech.DictationRecognizer
import com.brailly.friendly.data.speech.TextToSpeechManager
import com.brailly.friendly.ui.screen.learn.LearnViewModel
import com.brailly.friendly.ui.screen.scan.ScanHistoryViewModel
import com.brailly.friendly.ui.screen.scan.ScanViewModel
import com.brailly.friendly.ui.screen.translate.TranslateViewModel
import androidx.room.Room
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

    // Suara
    single { TextToSpeechManager(context = androidContext()) }
    single { DictationRecognizer(context = androidContext()) }

    // Room: basis data + DAO + repository progres belajar & riwayat scan.
    single {
        Room.databaseBuilder(
            androidContext(),
            BraillyDatabase::class.java,
            BraillyDatabase.NAME,
        ).build()
    }
    single { get<BraillyDatabase>().levelResultDao() }
    single { get<BraillyDatabase>().videoProgressDao() }
    single { get<BraillyDatabase>().scanHistoryDao() }
    single { LearnProgressRepository(dao = get(), videoDao = get()) }
    singleOf(::ScanHistoryRepository)

    // ViewModels
    viewModelOf(::TranslateViewModel)
    viewModelOf(::LearnViewModel)
    viewModelOf(::ScanViewModel)
    viewModelOf(::ScanHistoryViewModel)
}
