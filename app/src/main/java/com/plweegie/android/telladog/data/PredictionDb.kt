package com.plweegie.android.telladog.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [DogPrediction::class], version = 4)
abstract class PredictionDb : RoomDatabase() {

    abstract fun predictionDao(): PredictionDao
}