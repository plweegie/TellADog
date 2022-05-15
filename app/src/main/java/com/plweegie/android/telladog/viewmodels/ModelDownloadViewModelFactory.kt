package com.plweegie.android.telladog.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.mlkit.common.model.CustomRemoteModel
import com.google.mlkit.common.model.DownloadConditions
import javax.inject.Provider

class ModelDownloadViewModelFactory(
    private val downloadConditions: Provider<DownloadConditions>,
    private val remoteModel: Provider<CustomRemoteModel>
) : ViewModelProvider.NewInstanceFactory() {

    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        ModelDownloadViewModel(downloadConditions.get(), remoteModel.get()) as T
}