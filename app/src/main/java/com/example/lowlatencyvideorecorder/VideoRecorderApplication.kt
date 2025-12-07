package com.example.lowlatencyvideorecorder

import android.app.Application
import com.example.lowlatencyvideorecorder.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class VideoRecorderApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@VideoRecorderApplication)
            modules(appModule)
        }
    }
}

