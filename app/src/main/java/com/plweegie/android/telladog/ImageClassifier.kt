package com.plweegie.android.telladog

import android.app.Activity
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.*
import kotlin.collections.ArrayList


class ImageClassifier @Throws(IOException::class) constructor(val mActivity: Activity) {

    private val intValues = IntArray(DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y)

    private var tflite: Interpreter? = null
    private var labelList: MutableList<String>

    private var imgData: ByteBuffer

    private var labelProbArray: Array<FloatArray>
    private var filterLabelProbArray: Array<FloatArray>

    private val sortedLabels = PriorityQueue<Map.Entry<String, Float>>(
            RESULTS_TO_SHOW,
            kotlin.Comparator { o1, o2 ->  o1.value.compareTo(o2.value)}
    )

    init {
        tflite = Interpreter(loadModelFile(mActivity))
        labelList = loadLabelList(mActivity)

        imgData = ByteBuffer.allocateDirect(4 * DIM_BATCH_SIZE * DIM_IMG_SIZE_X *
            DIM_IMG_SIZE_Y * DIM_PIXEL_SIZE)
        imgData.order(ByteOrder.nativeOrder())

        labelProbArray = Array(1, { FloatArray(labelList.size) } )
        filterLabelProbArray = Array(FILTER_STAGES, { FloatArray(labelList.size) })
        Log.d(TAG, "Created a Tensorflow Lite Image Classifier.")
    }

    fun classifyFrame(bitmap: Bitmap): String {
        if (tflite == null) {
            Log.e(TAG, "Image classifier has not been initialized; Skipped.")
            return "Uninitialized classifier"
        }

        convertBitmapToByteBuffer(bitmap)
        tflite?.run(imgData, labelProbArray)
        applyFilter()

        return printTopKLabels()
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
        tflite?.close()
        tflite = null
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

    @Throws(IOException::class)
    private fun loadModelFile(activity: Activity): MappedByteBuffer {
        val fileDescriptor = activity.assets.openFd(MODEL_PATH)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel

        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
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

    private fun printTopKLabels(): String {
        for (i in 0 until labelList.size) {
            sortedLabels.add(
                    AbstractMap.SimpleEntry(labelList[i], labelProbArray[0][i]))
            if (sortedLabels.size > RESULTS_TO_SHOW) {
                sortedLabels.poll()
            }
        }
        var textToShow = ""
        val size = sortedLabels.size
        for (i in 0 until size) {
            val label = sortedLabels.poll()
            textToShow = String.format("%s", label.key) + textToShow
        }
        return textToShow
    }


    companion object {
        private const val TAG = "ImageClassifier"
        private const val MODEL_PATH = "dog_optimized_graph.lite"
        private const val LABEL_PATH = "dog_labels.txt"

        private const val RESULTS_TO_SHOW = 1
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