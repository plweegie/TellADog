package com.plweegie.android.telladog.di

import com.plweegie.android.telladog.ImageClassifier
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [MachineLearningModule::class])
interface MachineLearningComponent {
    fun inject(classifier: ImageClassifier)
}