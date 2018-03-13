package com.plweegie.android.telladog

import android.app.Application
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class MyAppModule(private val mApplication: Application) {

    @Provides
    @Singleton
    fun providesApplication(): Application = mApplication
}