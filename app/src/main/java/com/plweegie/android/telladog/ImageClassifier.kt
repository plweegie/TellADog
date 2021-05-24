/* Copyright 2017 The TensorFlow Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

Modifications (C) 2018 Jan K Szymanski
==============================================================================*/

package com.plweegie.android.telladog

import android.app.Activity
import android.graphics.Bitmap
import android.util.Log
import android.widget.Toast
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabel
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.custom.CustomImageLabelerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference
import javax.inject.Inject


class ImageClassifier(
    activity: Activity,
) {
    private val activityRef = WeakReference(activity)

    private var imageLabeler: ImageLabeler? = null

    @Inject
    lateinit var imageLabelerOptions: CustomImageLabelerOptions

    init {
        (activityRef.get()?.application as MyApp).machineLearningComponent.inject(this)

        imageLabeler = ImageLabeling.getClient(imageLabelerOptions)
    }

    fun getPredictions(bitmap: Bitmap): List<Pair<String, Float>>? {
        if (imageLabeler == null) {
            Log.e(TAG, "Image classifier has not been initialized; Skipped.")
            return null
        }

        val image = InputImage.fromBitmap(bitmap, 0)

        val topKPredictions = runBlocking {
            getPredictionsInternal(image)
        } ?: emptyList()

        return topKPredictions
            .map { Pair(it.text, it.confidence) }
    }

    private suspend fun getPredictionsInternal(image: InputImage): List<ImageLabel>? =
        try {
            imageLabeler?.process(image)?.await()
        } catch (e: MlKitException) {
            withContext(Dispatchers.Main) {
                Toast.makeText(activityRef.get(), e.message, Toast.LENGTH_SHORT).show()
            }
            null
        }

    fun close() {
        imageLabeler?.close()
        imageLabeler = null
    }

    companion object {
        private const val TAG = "ImageClassifier"

        const val DIM_IMG_SIZE_X = 224
        const val DIM_IMG_SIZE_Y = 224
    }
}