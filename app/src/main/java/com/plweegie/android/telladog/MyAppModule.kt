package com.plweegie.android.telladog

import android.app.Application
import android.content.Context
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class MyAppModule(private val mApplication: Application) {

    @Provides
    @Singleton
    fun providesApplication(): Application = mApplication

    @Provides
    @Singleton
    fun providesGlobalContext(): Context = mApplication.applicationContext
}