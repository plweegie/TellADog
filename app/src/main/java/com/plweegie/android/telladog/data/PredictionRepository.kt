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

    fun getAll(): LiveData<List<DogPrediction>> = mDatabase.predictionDao().getAllPredictions()
    
    fun add(prediction: DogPrediction) {
        executor.execute { mDatabase.predictionDao().insertPrediction(prediction) }
    }

    fun update(predictionId: Long?, syncState: Int) {
        executor.execute { mDatabase.predictionDao().updatePrediction(predictionId, syncState) }
    }

    fun delete(predictionId: Long?) {
        executor.execute { mDatabase.predictionDao().deletePrediction(predictionId) }
    }

    fun deleteAll() {
        executor.execute { mDatabase.predictionDao().deleteAll() }
    }

    fun syncToFirebase(prediction: DogPrediction?) {
        prediction?.syncState = DogPrediction.SyncState.SYNCING.value
        prediction?.let {
            update(it.timestamp, it.syncState)
        }

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
            val data = getImageDataFromFile(prediction.imageUri)

            dogImagesReference.putFile(file)
                    .addOnSuccessListener {
                        prediction.syncState = DogPrediction.SyncState.SYNCED.value
                    }
                    .addOnFailureListener {
                        prediction.syncState = DogPrediction.SyncState.NOT_SYNCED.value
                    }
                    .addOnCompleteListener {
                        update(prediction.timestamp, prediction.syncState)
                    }
        }
    }

    private fun getImageDataFromFile(filePath: String) {

    }
}