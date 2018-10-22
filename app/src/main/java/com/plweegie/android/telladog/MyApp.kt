package com.plweegie.android.telladog

import android.app.Application
import com.plweegie.android.telladog.di.*


class MyApp : Application() {

    val mAppComponent: MyAppComponent by lazy {
        DaggerMyAppComponent.builder()
                .myAppModule(MyAppModule(this))
                .roomModule(RoomModule(DATABASE_NAME))
                .firebaseModule(FirebaseModule())
                .build()
    }

    val machineLearningComponent: MachineLearningComponent by lazy {
        DaggerMachineLearningComponent.builder()
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