package com.plweegie.android.telladog.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.plweegie.android.telladog.data.DogPrediction
import com.plweegie.android.telladog.data.PredictionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch


class PredictionListViewModel(private val repository: PredictionRepository) : ViewModel() {

    private val mPredictionList = repository.getAll()

    private val viewModelJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    fun getPredictionList(): LiveData<List<DogPrediction>> = mPredictionList

    fun deletePrediction(prediction: DogPrediction?, userId: String) {
        uiScope.launch {
            repository.delete(prediction, userId)
        }
    }

    fun syncToFirebase(prediction: DogPrediction?, userId: String, isImageSyncAllowed: Boolean) {
        repository.syncToFirebase(prediction, userId, isImageSyncAllowed)
    }

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }
}