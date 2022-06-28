package com.unlone.app.android

import android.app.Application
import com.unlone.app.android.di.androidModule
import com.unlone.app.di.appModule
import org.koin.android.BuildConfig
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import timber.log.Timber.*
import timber.log.Timber.Forest.plant


class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            plant(DebugTree())
        }

        startKoin {
            androidContext(this@MainApplication)
            androidLogger()
            modules(appModule() + androidModule)
        }
    }
}
