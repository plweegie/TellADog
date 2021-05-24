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
                .machineLearningModule(MachineLearningModule(
                    CLOUD_MODEL_NAME,
                    MAX_NUMBER_OF_RESULTS,
                    CONFIDENCE_THRESHOLD
                ))
                .build()
    }

    override fun onCreate() {
        super.onCreate()
    }

    private companion object {
        const val DATABASE_NAME = "predictions"
        const val CLOUD_MODEL_NAME = "dbr_recognizer"
        const val MAX_NUMBER_OF_RESULTS = 3
        const val CONFIDENCE_THRESHOLD = 0.3f
    }
}