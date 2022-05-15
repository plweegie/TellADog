package com.plweegie.android.telladog.classifier

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow


class DogClassifierViewModel : ViewModel() {

    val results = MutableStateFlow<List<Pair<String, Float>>>(emptyList())
}