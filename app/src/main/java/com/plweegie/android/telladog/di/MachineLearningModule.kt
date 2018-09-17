package com.plweegie.android.telladog.di

import android.os.Build
import com.google.firebase.ml.custom.FirebaseModelDataType
import com.google.firebase.ml.custom.FirebaseModelInputOutputOptions
import com.google.firebase.ml.custom.FirebaseModelOptions
import com.google.firebase.ml.custom.model.FirebaseCloudModelSource
import com.google.firebase.ml.custom.model.FirebaseLocalModelSource
import com.google.firebase.ml.custom.model.FirebaseModelDownloadConditions
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class MachineLearningModule(private val mCloudModelName: String, private val mLocalModelName: String) {

    @Provides
    @Singleton
    fun provideMLConditions(): FirebaseModelDownloadConditions {
        var conditionsBuilder = FirebaseModelDownloadConditions.Builder().requireWifi()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            conditionsBuilder = conditionsBuilder
                    .requireCharging()
                    .requireDeviceIdle()
        }
        return conditionsBuilder.build()
    }

    @Provides
    @Singleton
    fun provideMLCloudSource(conditions: FirebaseModelDownloadConditions): FirebaseCloudModelSource =
            FirebaseCloudModelSource.Builder(mCloudModelName)
                    .enableModelUpdates(true)
                    .setInitialDownloadConditions(conditions)
                    .setUpdatesDownloadConditions(conditions)
                    .build()

    @Provides
    @Singleton
    fun provideMLLocalSource(): FirebaseLocalModelSource =
            FirebaseLocalModelSource.Builder("{$mCloudModelName}_local")
                    .setAssetFilePath(mLocalModelName)
                    .build()

    @Provides
    @Singleton
    fun provideMLOptions(): FirebaseModelOptions =
            FirebaseModelOptions.Builder()
                    .setCloudModelName(mCloudModelName)
                    .setLocalModelName("{$mCloudModelName}_local")
                    .build()

    @Provides
    @Singleton
    fun provideMLInOutOptions(): FirebaseModelInputOutputOptions =
            FirebaseModelInputOutputOptions.Builder()
                    .setInputFormat(0, FirebaseModelDataType.FLOAT32, intArrayOf(1, 224, 224, 3))
                    .setOutputFormat(0, FirebaseModelDataType.FLOAT32, intArrayOf(1, 120))
                    .build()


}