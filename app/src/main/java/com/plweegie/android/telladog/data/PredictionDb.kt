package com.plweegie.android.telladog.data

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase

@Database(entities = [DogPrediction::class], version = 3)
abstract class PredictionDb : RoomDatabase() {

    abstract fun predictionDao(): PredictionDao
}