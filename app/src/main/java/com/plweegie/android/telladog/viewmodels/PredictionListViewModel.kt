package com.plweegie.android.telladog.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.plweegie.android.telladog.data.DogPrediction
import com.plweegie.android.telladog.data.PredictionRepository
import kotlinx.coroutines.launch


class PredictionListViewModel(private val repository: PredictionRepository) : ViewModel() {

    private val mPredictionList = repository.getAll()

    fun getPredictionList(): LiveData<List<DogPrediction>> = mPredictionList

    fun deletePrediction(prediction: DogPrediction?, userId: String) {
        viewModelScope.launch {
            repository.delete(prediction, userId)
        }
    }

    fun syncToFirebase(prediction: DogPrediction?, userId: String, isImageSyncAllowed: Boolean) {
        repository.syncToFirebase(prediction, userId, isImageSyncAllowed)
    }
}