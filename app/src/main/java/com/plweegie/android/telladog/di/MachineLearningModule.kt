package com.plweegie.android.telladog.di

import com.google.firebase.ml.common.modeldownload.FirebaseLocalModel
import com.google.firebase.ml.common.modeldownload.FirebaseModelDownloadConditions
import com.google.firebase.ml.common.modeldownload.FirebaseRemoteModel
import com.google.firebase.ml.custom.FirebaseModelDataType
import com.google.firebase.ml.custom.FirebaseModelInputOutputOptions
import com.google.firebase.ml.custom.FirebaseModelOptions
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class MachineLearningModule(private val mCloudModelName: String, private val mLocalModelName: String) {

    @Provides
    @Singleton
    fun provideMLConditions(): FirebaseModelDownloadConditions {
        val conditionsBuilder = FirebaseModelDownloadConditions.Builder().requireWifi()
        return conditionsBuilder.build()
    }

    @Provides
    @Singleton
    fun provideMLCloudSource(conditions: FirebaseModelDownloadConditions): FirebaseRemoteModel =
            FirebaseRemoteModel.Builder(mCloudModelName)
                    .enableModelUpdates(true)
                    .setInitialDownloadConditions(conditions)
                    .setUpdatesDownloadConditions(conditions)
                    .build()

    @Provides
    @Singleton
    fun provideMLLocalSource(): FirebaseLocalModel =
            FirebaseLocalModel.Builder("{$mCloudModelName}_local")
                    .setAssetFilePath(mLocalModelName)
                    .build()

    @Provides
    @Singleton
    fun provideMLOptions(): FirebaseModelOptions =
            FirebaseModelOptions.Builder()
                    .setRemoteModelName(mCloudModelName)
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