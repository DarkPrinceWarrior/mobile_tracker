package com.example.mobile_tracker.di

import androidx.room.Room
import com.example.mobile_tracker.data.local.db.AppDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "activity_tracker_db",
        ).build()
    }

    single { get<AppDatabase>().employeeDao() }
    single { get<AppDatabase>().deviceDao() }
    single { get<AppDatabase>().bindingDao() }
    single { get<AppDatabase>().packetQueueDao() }
    single { get<AppDatabase>().operationLogDao() }
    single { get<AppDatabase>().siteDao() }
    single { get<AppDatabase>().shiftContextDao() }
}
