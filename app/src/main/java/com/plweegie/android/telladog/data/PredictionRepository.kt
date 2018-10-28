package com.plweegie.android.telladog.data

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.net.Uri
import com.google.firebase.database.DatabaseReference
import com.google.firebase.storage.StorageReference
import java.io.File
import java.util.*
import java.util.concurrent.ExecutorService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PredictionRepository @Inject constructor(private val mDatabase: PredictionDb,
                                               private val executor: ExecutorService,
                                               private val firebaseStorage: StorageReference,
                                               private val firebaseDatabase: DatabaseReference) {

    var isSendingToCloud: MutableLiveData<Boolean> = MutableLiveData()

    fun getAll(): LiveData<List<DogPrediction>> = mDatabase.predictionDao().getAllPredictions()
    
    fun add(prediction: DogPrediction) {
        executor.execute { mDatabase.predictionDao().insertPrediction(prediction) }
    }

    fun update(predictionId: Long?) {
        executor.execute { mDatabase.predictionDao().updatePrediction(predictionId) }
    }

    fun delete(predictionId: Long?) {
        executor.execute { mDatabase.predictionDao().deletePrediction(predictionId) }
    }

    fun deleteAll() {
        executor.execute { mDatabase.predictionDao().deleteAll() }
    }

    fun syncToFirebase(prediction: DogPrediction?) {
        isSendingToCloud.value = true

        sendToDatabase(prediction)
        sendToStorage(prediction)
    }

    fun sendToDatabase(prediction: DogPrediction?) {
        val guid = UUID.randomUUID().toString()

        firebaseDatabase.child(guid).setValue(prediction)
    }

    fun sendToStorage(prediction: DogPrediction?) {

        if (prediction != null) {
            val file = Uri.fromFile(File(prediction.imageUri))
            val dogImagesReference = firebaseStorage.child(file.lastPathSegment!!)

            dogImagesReference.putFile(file)
                    .addOnSuccessListener {
                        isSendingToCloud.value = false
                        update(prediction.timestamp)
                    }
        }
    }
}