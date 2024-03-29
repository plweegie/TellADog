package com.plweegie.android.telladog

import com.plweegie.android.telladog.camera.CameraXFragment
import com.plweegie.android.telladog.di.FirebaseModule
import com.plweegie.android.telladog.di.MachineLearningModule
import com.plweegie.android.telladog.di.RoomModule
import com.plweegie.android.telladog.ui.DogListFragment
import dagger.Component
import dagger.android.AndroidInjectionModule
import javax.inject.Singleton

@Singleton
@Component(modules = [MyAppModule::class, AndroidInjectionModule::class,  RoomModule::class, FirebaseModule::class, MachineLearningModule::class])
interface MyAppComponent {
    fun inject(fragment: DogListFragment)
    fun inject(activity: MainActivity)
    fun inject(fragment: CameraXFragment)
}