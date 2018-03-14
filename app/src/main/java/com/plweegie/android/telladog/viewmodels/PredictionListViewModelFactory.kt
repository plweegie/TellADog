package com.plweegie.android.telladog.viewmodels

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import com.plweegie.android.telladog.data.PredictionRepository


class PredictionListViewModelFactory(private val mRepository: PredictionRepository) :
        ViewModelProvider.NewInstanceFactory() {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T =
            PredictionListViewModel(mRepository) as T
}