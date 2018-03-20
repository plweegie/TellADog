package com.plweegie.android.telladog.data

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query

@Dao
interface PredictionDao {

    @Query("SELECT * FROM predictions")
    fun getAllPredictions(): LiveData<List<DogPrediction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPrediction(prediction: DogPrediction)

    @Query("DELETE FROM predictions WHERE id = :predictionId")
    fun deletePrediction(predictionId: Long?)

    @Query("DELETE FROM predictions")
    fun deleteAll()
}