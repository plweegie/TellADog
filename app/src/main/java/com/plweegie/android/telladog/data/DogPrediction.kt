package com.plweegie.android.telladog.data

import android.arch.persistence.room.*

@Entity(tableName = "predictions", indices = [(Index("timestamp"))])
data class DogPrediction(@PrimaryKey(autoGenerate = true) val id: Long?,
                         val prediction: String?,
                         val accuracy: Float?,
                         @ColumnInfo(name = "image_uri") val imageUri: String,
                         val timestamp: Long) {

    @Ignore
    constructor(): this(null, "", 0.0f, "", 0L)

    @Ignore
    constructor(prediction: String?,
                accuracy: Float?,
                imageUri: String,
                timestamp: Long):
            this(null, prediction, accuracy, imageUri, timestamp)
}