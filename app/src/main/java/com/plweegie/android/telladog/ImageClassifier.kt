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
import com.google.firebase.ml.common.FirebaseMLException
import com.google.firebase.ml.custom.*
import com.google.firebase.ml.custom.model.FirebaseCloudModelSource
import com.google.firebase.ml.custom.model.FirebaseLocalModelSource
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


class ImageClassifier @Throws(IOException::class) constructor(val mActivity: Activity) {

    private val intValues = IntArray(DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y)

    private var labelList: MutableList<String>
    private var mFirebaseInterpreter: FirebaseModelInterpreter? = null

    @Inject
    lateinit var mCloudSource: FirebaseCloudModelSource

    @Inject
    lateinit var mLocalSource: FirebaseLocalModelSource

    @Inject
    lateinit var mModelOptions: FirebaseModelOptions

    @Inject
    lateinit var mModelInputOutputOptions: FirebaseModelInputOutputOptions

    private var imgData: ByteBuffer

    private var labelProbArray: Array<FloatArray>
    private var filterLabelProbArray: Array<FloatArray>

    private val sortedLabels = PriorityQueue<Map.Entry<String, Float>>(
            RESULTS_TO_SHOW,
            kotlin.Comparator { o1, o2 ->  o1.value.compareTo(o2.value)}
    )

    init {
        (mActivity.application as MyApp).mAppComponent.inject(this)

        FirebaseModelManager.getInstance().apply {
            registerCloudModelSource(mCloudSource)
            registerLocalModelSource(mLocalSource)
        }
        
        labelList = loadLabelList(mActivity)
        mFirebaseInterpreter = FirebaseModelInterpreter.getInstance(mModelOptions)

        imgData = ByteBuffer.allocateDirect(4 * DIM_BATCH_SIZE * DIM_IMG_SIZE_X *
            DIM_IMG_SIZE_Y * DIM_PIXEL_SIZE)
        imgData.order(ByteOrder.nativeOrder())

        labelProbArray = Array(1, { FloatArray(labelList.size) } )
        filterLabelProbArray = Array(FILTER_STAGES, { FloatArray(labelList.size) })
    }

    fun getPredictions(bitmap: Bitmap): List<Pair<String, Float>>? {
        if (mFirebaseInterpreter == null) {
            Log.e(TAG, "Image classifier has not been initialized; Skipped.")
            return null
        }

        convertBitmapToByteBuffer(bitmap)

        val inputs = FirebaseModelInputs.Builder()
                .add(imgData)
                .build()

        mFirebaseInterpreter?.run(inputs, mModelInputOutputOptions)?.addOnSuccessListener {
            outputs -> labelProbArray = outputs.getOutput<Array<FloatArray>>(0)
        }?.addOnFailureListener {
            Log.e("Interpreter", "Error getting predictions, code ${(it as FirebaseMLException).code}")
            it.printStackTrace()
        }

        applyFilter()

        return getTopKLabels()
    }

    private fun applyFilter() {
        val numLabels = labelList.size

        for (j in 0 until numLabels) {
            filterLabelProbArray[0][j] += FILTER_FACTOR * (labelProbArray[0][j] -
                    filterLabelProbArray[0][j])
        }

        for (i in 1 until FILTER_STAGES) {
            for (j in 0 until numLabels) {
                filterLabelProbArray[i][j] += FILTER_FACTOR * (filterLabelProbArray[i - 1][j]
                        - filterLabelProbArray[i][j])

            }
        }

        for (j in 0 until numLabels) {
            labelProbArray[0][j] = filterLabelProbArray[FILTER_STAGES - 1][j]
        }
    }

    fun close() {
        mFirebaseInterpreter?.close()
        mFirebaseInterpreter = null
    }

    @Throws(IOException::class)
    private fun loadLabelList(activity: Activity): MutableList<String> {
        val labelList = ArrayList<String>()
        val reader = BufferedReader(InputStreamReader(activity.assets.open(LABEL_PATH)))

        var line = reader.readLine()
        while (line != null) {
            labelList.add(line)
            line = reader.readLine()
        }

        reader.close()
        return labelList
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap) {

        imgData.rewind()
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        var pixel = 0

        for (i in 0 until DIM_IMG_SIZE_X) {
            for (j in 0 until DIM_IMG_SIZE_Y) {
                val value = intValues[pixel++]
                imgData.putFloat(((value shr 16 and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                imgData.putFloat(((value shr 8 and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                imgData.putFloat(((value and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
            }
        }
    }

    private fun getTopKLabels(): List<Pair<String, Float>> {
        for (i in 0 until labelList.size) {
            sortedLabels.add(
                    AbstractMap.SimpleEntry(labelList[i], labelProbArray[0][i]))
            if (sortedLabels.size > RESULTS_TO_SHOW) {
                sortedLabels.poll()
            }
        }

        val mapToOutput = HashMap<String, Float>()
        val size = sortedLabels.size
        for (i in 0 until size) {
            val label = sortedLabels.poll()
            mapToOutput[label.key] = label.value
        }

        return mapToOutput.toList()
                .sortedByDescending { (k, v) -> v }
    }


    companion object {
        private const val TAG = "ImageClassifier"
        private const val LABEL_PATH = "dog_labels.txt"

        private const val RESULTS_TO_SHOW = 3
        private const val DIM_BATCH_SIZE = 1
        private const val DIM_PIXEL_SIZE = 3

        //Important - will have to be modified
        const val DIM_IMG_SIZE_X = 224
        const val DIM_IMG_SIZE_Y = 224

        private const val IMAGE_MEAN = 128
        private const val IMAGE_STD = 128.0f

        private const val FILTER_STAGES = 3
        private const val FILTER_FACTOR = 0.4f
    }
}