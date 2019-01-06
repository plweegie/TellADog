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

    fun deletePrediction(predictionID: Long) {
        uiScope.launch {
            repository.delete(predictionID)
        }
    }

    fun syncToFirebase(prediction: DogPrediction?) {
        repository.syncToFirebase(prediction)
    }

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }
}