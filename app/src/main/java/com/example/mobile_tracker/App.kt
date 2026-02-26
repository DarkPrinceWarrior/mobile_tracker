package com.example.mobile_tracker

import android.app.Application
import com.example.mobile_tracker.di.appModule
import com.example.mobile_tracker.di.databaseModule
import com.example.mobile_tracker.di.networkModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import timber.log.Timber

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        startKoin {
            androidLogger()
            androidContext(this@App)
            modules(
                appModule,
                networkModule,
                databaseModule,
            )
        }
    }
}
