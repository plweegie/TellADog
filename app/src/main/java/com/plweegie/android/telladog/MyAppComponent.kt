package com.plweegie.android.telladog

import com.plweegie.android.telladog.camera.CameraFragment
import com.plweegie.android.telladog.di.FirebaseModule
import com.plweegie.android.telladog.di.RoomModule
import com.plweegie.android.telladog.ui.DogListFragment
import dagger.Component
import dagger.android.AndroidInjectionModule
import javax.inject.Singleton

@Singleton
@Component(modules = [MyAppModule::class, AndroidInjectionModule::class,  RoomModule::class, FirebaseModule::class])
interface MyAppComponent {
    fun inject(fragment: DogListFragment)
    fun inject(cameraFragment: CameraFragment)
}