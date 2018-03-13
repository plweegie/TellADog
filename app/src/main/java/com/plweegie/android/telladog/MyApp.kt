package com.plweegie.android.telladog

import android.app.Application
import com.plweegie.android.telladog.data.RoomModule


class MyApp : Application() {

    val mAppComponent: MyAppComponent by lazy {
        DaggerMyAppComponent.builder()
                .myAppModule(MyAppModule(this))
                .roomModule(RoomModule(DATABASE_NAME))
                .build()
    }

    override fun onCreate() {
        super.onCreate()
    }

    companion object {
        const val DATABASE_NAME = "predictions"
    }
}