package com.plweegie.android.telladog.data

import android.app.Application
import android.arch.persistence.room.Room
import com.plweegie.android.telladog.viewmodels.PredictionListViewModelFactory
import dagger.Module
import dagger.Provides
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Singleton

@Module
class RoomModule(private val mDatabaseName: String) {

    @Provides
    @Singleton
    fun provideDatabase(application: Application): PredictionDb  =
            Room.databaseBuilder(application, PredictionDb::class.java, mDatabaseName).build()

    @Provides
    @Singleton
    fun provideDiskIOExecutor(): ExecutorService = Executors.newSingleThreadExecutor()

    @Provides
    @Singleton
    fun provideViewModelFactory(repository: PredictionRepository): PredictionListViewModelFactory =
            PredictionListViewModelFactory(repository)
}