package com.plweegie.android.telladog.data

import android.app.Application
import android.arch.persistence.room.Room
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class RoomModule(private val mDatabaseName: String) {

    @Provides
    @Singleton
    fun provideDatabase(application: Application)  =
            Room.databaseBuilder(application, PredictionDb::class.java, mDatabaseName).build()
}