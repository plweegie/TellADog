package com.plweegie.android.telladog.viewmodels

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.ViewModel
import com.plweegie.android.telladog.data.DogPrediction
import com.plweegie.android.telladog.data.PredictionRepository
import kotlinx.coroutines.*


class PredictionListViewModel(private val repository: PredictionRepository) : ViewModel() {

    private val mPredictionList = repository.getAll()

    private val viewModelJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    fun getPredictionList(): LiveData<List<DogPrediction>> = mPredictionList

    fun deletePrediction(prediction: DogPrediction?) {
        uiScope.launch {
            repository.delete(prediction)
        }
    }

    fun syncToFirebase(prediction: DogPrediction?, isImageSyncAllowed: Boolean) {
        repository.syncToFirebase(prediction, isImageSyncAllowed)
    }

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }
}