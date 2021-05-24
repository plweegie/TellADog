package com.plweegie.android.telladog.di

import com.plweegie.android.telladog.ImageClassifier
import com.plweegie.android.telladog.MainActivity
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [MachineLearningModule::class])
interface MachineLearningComponent {
    fun inject(activity: MainActivity)
    fun inject(classifier: ImageClassifier)
}