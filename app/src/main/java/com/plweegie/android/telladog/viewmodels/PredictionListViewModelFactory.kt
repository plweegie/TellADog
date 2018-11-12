package com.plweegie.android.telladog.viewmodels

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import com.plweegie.android.telladog.data.PredictionRepository
import javax.inject.Provider


class PredictionListViewModelFactory(private val mRepository: Provider<PredictionRepository>) :
        ViewModelProvider.NewInstanceFactory() {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T =
            PredictionListViewModel(mRepository.get()) as T
}