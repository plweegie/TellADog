package com.plweegie.android.telladog

import com.plweegie.android.telladog.camera.CameraFragment
import com.plweegie.android.telladog.di.MachineLearningModule
import com.plweegie.android.telladog.di.RoomModule
import com.plweegie.android.telladog.ui.DogListFragment
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [MyAppModule::class, RoomModule::class, MachineLearningModule::class])
interface MyAppComponent {
    fun inject(fragment: DogListFragment)
    fun inject(cameraFragment: CameraFragment)
    fun inject(classifier: ImageClassifier)
}