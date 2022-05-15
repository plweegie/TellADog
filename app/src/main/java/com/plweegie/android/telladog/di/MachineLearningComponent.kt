package com.plweegie.android.telladog.di

import com.plweegie.android.telladog.MainActivity
import com.plweegie.android.telladog.camera.CameraXFragment
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [MachineLearningModule::class])
interface MachineLearningComponent {
    fun inject(activity: MainActivity)
    fun inject(fragment: CameraXFragment)
}