package com.plweegie.android.telladog.data

import android.arch.lifecycle.LiveData
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.google.firebase.database.*
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
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

    private fun update(predictionId: Long?, syncState: Int) {
        executor.execute { mDatabase.predictionDao().updatePrediction(predictionId, syncState) }
    }

    suspend fun delete(prediction: DogPrediction?) {
        executor.execute { mDatabase.predictionDao().deletePrediction(prediction?.timestamp) }

        prediction?.run {
            deleteFromDatabase(timestamp!!)
            deleteFromStorage(imageUri!!)
        }
    }

    fun deleteAll() {
        executor.execute { mDatabase.predictionDao().deleteAll() }
    }

    fun syncToFirebase(prediction: DogPrediction?, isImageSyncAllowed: Boolean) {
        prediction?.syncState = DogPrediction.SyncState.SYNCING.value
        prediction?.run {
            update(timestamp, syncState)
        }

        sendToDatabase(prediction)

        if (isImageSyncAllowed) {
            sendToStorage(prediction)
        }
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

        prediction?.run {
            val file = Uri.fromFile(File(imageUri))
            val dogImagesReference = firebaseStorage.child(file.lastPathSegment!!)
            val data = getImageDataFromFile(imageUri!!)

            dogImagesReference.putBytes(data)
                    .addOnSuccessListener {
                        syncState = DogPrediction.SyncState.SYNCED.value
                    }
                    .addOnFailureListener {
                        syncState = DogPrediction.SyncState.NOT_SYNCED.value
                    }
                    .addOnCompleteListener {
                        update(timestamp, syncState)
                    }
        }
    }

    private suspend fun deleteFromDatabase(predictionId: Long) {
        withContext(Dispatchers.Default) {
            try {
                val result = firebaseDatabase
                        .orderByChild("timestamp")
                        .equalTo(predictionId.toDouble())
                        .await()

                result.children.forEach {
                    it.ref.removeValue()
                }
            } catch (e: Exception) {
                Log.e("PredictionRepository", "Error deleting entry")
            }
        }
    }

    private suspend fun deleteFromStorage(predictionImageUri: String) {
        val storageChildReference = predictionImageUri.substringAfterLast("/")

        withContext(Dispatchers.Default) {
            try {
                firebaseStorage.child(storageChildReference).delete().await()
            } catch (e: StorageException) {
                Log.e("PredictionRepository", "Error deleting entry image")
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