package com.plweegie.android.telladog.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.common.MlKitException
import com.google.mlkit.common.model.CustomRemoteModel
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.asDeferred
import kotlinx.coroutines.tasks.await


class ModelDownloadViewModel(
    private val downloadConditions: DownloadConditions,
    private val remoteModel: CustomRemoteModel
) : ViewModel() {

    private val _modelDownloadState = MutableStateFlow(DownloadState.IDLE)

    val modelDownloadState: StateFlow<DownloadState> = _modelDownloadState

    fun isRemoteClassifierModelDownloadedAsync(): Deferred<Boolean> =
        RemoteModelManager.getInstance().isModelDownloaded(remoteModel).asDeferred()

    fun downloadRemoteClassifierModel() {
        viewModelScope.launch {
            _modelDownloadState.value = DownloadState.IN_PROGRESS

            try {
                RemoteModelManager.getInstance().download(remoteModel, downloadConditions).await()
                _modelDownloadState.value = DownloadState.COMPLETE
            } catch (e: MlKitException) {
                Log.e("ModelDownload", "Image classifier could not be downloaded: $e")
            }
        }
    }

    enum class DownloadState {
        IDLE,
        IN_PROGRESS,
        COMPLETE
    }
}