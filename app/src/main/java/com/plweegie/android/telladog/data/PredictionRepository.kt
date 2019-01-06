package com.plweegie.android.telladog.data

import android.arch.lifecycle.LiveData
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.google.firebase.database.*
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.*
import java.util.concurrent.ExecutorService
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

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

    suspend fun delete(predictionId: Long?) {
        executor.execute { mDatabase.predictionDao().deletePrediction(predictionId) }

        predictionId?.run {
            withContext(Dispatchers.Default) {
                try {
                    val result = firebaseDatabase
                            .orderByChild("timestamp")
                            .equalTo(this@run.toDouble())
                            .await()

                    result.children.forEach {
                        it.ref.removeValue()
                    }
                } catch (e: Exception) {
                    Log.e("PredictionRepository", "Error deleting entry")
                }
            }
        }
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

    private suspend fun Query.await(): DataSnapshot {
        return suspendCoroutine { continuation ->
            addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    continuation.resume(snapshot)
                }

                override fun onCancelled(error: DatabaseError) {
                   continuation.resumeWithException(error.toException())
                }
            })
        }
    }

    private fun sendToDatabase(prediction: DogPrediction?) {
        val guid = UUID.randomUUID().toString()

        firebaseDatabase.child(guid).setValue(prediction)
    }

    private fun sendToStorage(prediction: DogPrediction?) {

        if (prediction != null) {
            val file = Uri.fromFile(File(prediction.imageUri))
            val dogImagesReference = firebaseStorage.child(file.lastPathSegment!!)
            val data = getImageDataFromFile(prediction.imageUri!!)

            dogImagesReference.putBytes(data)
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

    private fun getImageDataFromFile(filePath: String): ByteArray {

        val outputStream = ByteArrayOutputStream()
        val options = BitmapFactory.Options().apply { inSampleSize = 4 }
        val bitmap = BitmapFactory.decodeFile(filePath, options)

        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
        return outputStream.toByteArray()
    }
}