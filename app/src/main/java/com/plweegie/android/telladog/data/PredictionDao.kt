package com.plweegie.android.telladog.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PredictionDao {

    @Query("SELECT * FROM predictions")
    fun getAllPredictions(): LiveData<List<DogPrediction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPrediction(prediction: DogPrediction)

    @Query("UPDATE predictions SET sync_state = :syncState WHERE timestamp = :predictionId")
    fun updatePrediction(predictionId: Long?, syncState: Int)

    @Query("DELETE FROM predictions WHERE timestamp = :predictionId")
    fun deletePrediction(predictionId: Long?)

    @Query("DELETE FROM predictions")
    fun deleteAll()
}