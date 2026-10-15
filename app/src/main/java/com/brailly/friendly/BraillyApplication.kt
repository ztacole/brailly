package com.brailly.friendly

import android.app.Application
import com.brailly.friendly.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class BraillyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@BraillyApplication)
            modules(appModule)
        }
    }
}
