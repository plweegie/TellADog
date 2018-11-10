package com.plweegie.android.telladog.viewmodels

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.ViewModel
import com.plweegie.android.telladog.data.DogPrediction
import com.plweegie.android.telladog.data.PredictionRepository


class PredictionListViewModel(private val repository: PredictionRepository) : ViewModel() {

    private val mPredictionList = repository.getAll()

    fun getPredictionList(): LiveData<List<DogPrediction>> = mPredictionList

    fun deletePrediction(predictionID: Long) {
        repository.delete(predictionID)
    }

    fun syncToFirebase(prediction: DogPrediction?) {
        repository.syncToFirebase(prediction)
    }
}