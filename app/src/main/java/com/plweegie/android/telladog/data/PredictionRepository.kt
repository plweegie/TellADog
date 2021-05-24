package com.plweegie.android.telladog.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import com.google.firebase.database.*
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
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

    suspend fun delete(prediction: DogPrediction?, userId: String) {
        executor.execute { mDatabase.predictionDao().deletePrediction(prediction?.timestamp) }

        prediction?.run {
            deleteFromDatabase(timestamp, userId)
            deleteFromStorage(imageUri!!, userId)
        }
    }

    fun syncToFirebase(prediction: DogPrediction?, userId: String, isImageSyncAllowed: Boolean) {
        prediction?.syncState = DogPrediction.SyncState.SYNCING.value
        prediction?.run {
            update(timestamp, syncState)
        }

        sendToDatabase(prediction, userId)

        if (isImageSyncAllowed) {
            sendToStorage(prediction, userId)
        }
    }

    private suspend fun Query.await(): DataSnapshot {
        return suspendCancellableCoroutine { continuation ->
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

    private fun sendToDatabase(prediction: DogPrediction?, userId: String) {
        val guid = UUID.randomUUID().toString()

        firebaseDatabase
                .child("users")
                .child(userId)
                .child(guid)
                .setValue(prediction)
    }

    private fun sendToStorage(prediction: DogPrediction?, userId: String) {

        prediction?.run {
            val file = Uri.fromFile(File(imageUri))
            val dogImagesReference = firebaseStorage
                    .child("images")
                    .child(userId)
                    .child(file.lastPathSegment!!)
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

    private suspend fun deleteFromDatabase(predictionId: Long, userId: String) {
        withContext(Dispatchers.Default) {
            try {
                val result = firebaseDatabase
                        .child("users")
                        .child(userId)
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

    private suspend fun deleteFromStorage(predictionImageUri: String, userId: String) {
        val storageChildReference = predictionImageUri.substringAfterLast("/")

        withContext(Dispatchers.Default) {
            try {
                firebaseStorage
                        .child("images")
                        .child(userId)
                        .child(storageChildReference)
                        .delete().await()
            } catch (e: Throwable) {
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