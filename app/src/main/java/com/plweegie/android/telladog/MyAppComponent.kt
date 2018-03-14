package com.plweegie.android.telladog

import com.plweegie.android.telladog.data.RoomModule
import com.plweegie.android.telladog.ui.DogListFragment
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [MyAppModule::class, RoomModule::class])
interface MyAppComponent {
    fun inject(fragment: DogListFragment)
}