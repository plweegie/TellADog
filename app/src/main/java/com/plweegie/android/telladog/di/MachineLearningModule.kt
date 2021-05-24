package com.plweegie.android.telladog.di

import com.google.mlkit.common.model.CustomRemoteModel
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelSource
import com.google.mlkit.linkfirebase.FirebaseModelSource
import com.google.mlkit.vision.label.custom.CustomImageLabelerOptions
import com.plweegie.android.telladog.data.PredictionRepository
import com.plweegie.android.telladog.viewmodels.ModelDownloadViewModelFactory
import com.plweegie.android.telladog.viewmodels.PredictionListViewModelFactory
import dagger.Module
import dagger.Provides
import javax.inject.Provider
import javax.inject.Singleton

@Module
class MachineLearningModule(
    private val cloudModelName: String,
    private val maxNumberOfResults: Int,
    private val confidenceThreshold: Float
) {

    @Provides
    @Singleton
    fun provideMLConditions(): DownloadConditions {
        val conditionsBuilder = DownloadConditions.Builder().requireWifi()
        return conditionsBuilder.build()
    }

    @Provides
    @Singleton
    fun provideMLCloudSource(): RemoteModelSource =
            FirebaseModelSource.Builder(cloudModelName)
                    .build()

    @Provides
    @Singleton
    fun provideCustomRemoteModel(remoteModelSource: RemoteModelSource): CustomRemoteModel =
            CustomRemoteModel.Builder(remoteModelSource)
                .build()

    @Provides
    @Singleton
    fun provideImageLabelerOptions(customRemoteModel: CustomRemoteModel): CustomImageLabelerOptions =
            CustomImageLabelerOptions.Builder(customRemoteModel)
                .setMaxResultCount(maxNumberOfResults)
                .setConfidenceThreshold(confidenceThreshold)
                .build()

    @Provides
    @Singleton
    fun provideModelDownloadViewModelFactory(
        downloadConditions: Provider<DownloadConditions>,
        customRemoteModel: Provider<CustomRemoteModel>
    ) = ModelDownloadViewModelFactory(downloadConditions, customRemoteModel)
}