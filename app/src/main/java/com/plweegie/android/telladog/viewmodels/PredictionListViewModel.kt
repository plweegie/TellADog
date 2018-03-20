package com.plweegie.android.telladog.viewmodels

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.ViewModel
import com.plweegie.android.telladog.data.DogPrediction
import com.plweegie.android.telladog.data.PredictionRepository


class PredictionListViewModel(repository: PredictionRepository) : ViewModel() {

    private val mPredictionList = repository.getAll()

    fun getPredictionList(): LiveData<List<DogPrediction>> = mPredictionList
}