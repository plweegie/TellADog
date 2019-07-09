package com.plweegie.android.telladog

import android.app.Application
import com.plweegie.android.telladog.di.FirebaseModule
import com.plweegie.android.telladog.di.MachineLearningModule
import com.plweegie.android.telladog.di.RoomModule


class MyApp : Application() {

    val mAppComponent: MyAppComponent by lazy {
        DaggerMyAppComponent.builder()
                .myAppModule(MyAppModule(this))
                .roomModule(RoomModule(DATABASE_NAME))
                .firebaseModule(FirebaseModule())
                .machineLearningModule(MachineLearningModule(CLOUD_MODEL_NAME, LOCAL_MODEL_NAME))
                .build()
    }

    override fun onCreate() {
        super.onCreate()
    }

    companion object {
        const val DATABASE_NAME = "predictions"
        const val LOCAL_MODEL_NAME = "dog_optimized_graph.tflite"
        const val CLOUD_MODEL_NAME = "dbr_recognizer"
    }
}