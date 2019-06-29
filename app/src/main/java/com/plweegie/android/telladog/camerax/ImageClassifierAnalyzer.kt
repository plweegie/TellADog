package com.plweegie.android.telladog.camerax


import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.firebase.ml.common.FirebaseMLException
import com.google.firebase.ml.common.modeldownload.FirebaseLocalModel
import com.google.firebase.ml.common.modeldownload.FirebaseModelManager
import com.google.firebase.ml.common.modeldownload.FirebaseRemoteModel
import com.google.firebase.ml.custom.FirebaseModelInputOutputOptions
import com.google.firebase.ml.custom.FirebaseModelInputs
import com.google.firebase.ml.custom.FirebaseModelInterpreter
import com.google.firebase.ml.custom.FirebaseModelOptions
import com.plweegie.android.telladog.utils.ThumbnailLoader
import java.io.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.Comparator
import kotlin.experimental.and


class ImageClassifierAnalyzer @Inject constructor(
        context: Context,
        cloudSource: FirebaseRemoteModel,
        localSource: FirebaseLocalModel,
        modelOptions: FirebaseModelOptions,
        private val modelInputOutputOptions: FirebaseModelInputOutputOptions) : ImageAnalysis.Analyzer {

    companion object {
        private const val TAG = "ImageClassifierAnalyzer"
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

    private val frameRateWindow = 8
    private val frameTimestamps = ArrayDeque<Long>(5)
    private var lastAnalyzedTimestamp = 0L
    var framesPerSecond: Double = -1.0
        private set

    private val listeners = ArrayList<(predictions: List<Pair<String, Float>>) -> Unit>()

    private val intValues = IntArray(DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y)

    private var labelList: MutableList<String> = mutableListOf()
    private var mFirebaseInterpreter: FirebaseModelInterpreter? = null

    private var imgData: ByteBuffer
    private var labelProbArray: Array<FloatArray>
    private var filterLabelProbArray: Array<FloatArray>

    private val sortedLabels = PriorityQueue<Map.Entry<String, Float>>(
            RESULTS_TO_SHOW,
            Comparator { o1, o2 ->  o1.value.compareTo(o2.value)}
    )

    init {
        FirebaseModelManager.getInstance().apply {
            registerRemoteModel(cloudSource)
            registerLocalModel(localSource)
        }

        labelList = loadLabelList(context)
        mFirebaseInterpreter = FirebaseModelInterpreter.getInstance(modelOptions)

        imgData = ByteBuffer.allocateDirect(4 * DIM_BATCH_SIZE * DIM_IMG_SIZE_X *
                DIM_IMG_SIZE_Y * DIM_PIXEL_SIZE)
        imgData.order(ByteOrder.nativeOrder())

        labelProbArray = Array(1) { FloatArray(labelList.size) }
        filterLabelProbArray = Array(FILTER_STAGES) { FloatArray(labelList.size) }
    }

    fun onFrameAnalyzed(listener: (predictions: List<Pair<String, Float>>) -> Unit) =
            listeners.add(listener)

    override fun analyze(image: ImageProxy?, rotationDegrees: Int) {
        if (mFirebaseInterpreter == null) {
            Log.e(TAG, "Image classifier has not been initialized; Skipped.")
            return
        }

        frameTimestamps.push(System.currentTimeMillis())
        while (frameTimestamps.size >= frameRateWindow) frameTimestamps.removeLast()
        framesPerSecond = 1.0 / ((frameTimestamps.peekFirst() -
                frameTimestamps.peekLast())  / frameTimestamps.size.toDouble()) * 1000.0

        if (frameTimestamps.first - lastAnalyzedTimestamp >= TimeUnit.SECONDS.toMillis(1)) {
            convertImageToByteBuffer(image)

            val inputs = FirebaseModelInputs.Builder()
                    .add(imgData)
                    .build()

            mFirebaseInterpreter?.run(inputs, modelInputOutputOptions)?.addOnSuccessListener {
                outputs -> labelProbArray = outputs.getOutput<Array<FloatArray>>(0)
            }?.addOnFailureListener {
                Log.e("Interpreter", "Error getting predictions, code ${(it as FirebaseMLException).code}")
                it.printStackTrace()
            }

            applyFilter()

            listeners.forEach {
                it(getTopKLabels())
            }

            lastAnalyzedTimestamp = frameTimestamps.first
        }
    }

    private fun convertImageToByteBuffer(image: ImageProxy?) {

        image?.run {
            imgData.rewind()

            val uBuffer = planes[1].buffer
            val vBuffer = planes[2].buffer
            val uData = uBuffer.toByteArray()
            val vData = vBuffer.toByteArray()
            val pixels = (uData + vData).map { it.toInt() }.toIntArray()
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            var pixel = 0

//            bitmap?.let { safeBitmap ->
//                safeBitmap.getPixels(intValues, 0, safeBitmap.width, 0, 0,
//                        safeBitmap.width, safeBitmap.height)
//                var pixel = 0
//
//                for (i in 0 until DIM_IMG_SIZE_X) {
//                    for (j in 0 until DIM_IMG_SIZE_Y) {
//                        val value = intValues[pixel++]
//                        imgData.putFloat(((value shr 16 and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
//                        imgData.putFloat(((value shr 8 and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
//                        imgData.putFloat(((value and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
//                    }
//                }
//
//                safeBitmap.recycle()
            //}
        }
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
                .sortedByDescending { (_, v) -> v }
    }

    private fun ByteBuffer.toByteArray(): ByteArray {
        rewind()    // Rewind the buffer to zero
        val data = ByteArray(remaining())
        get(data)   // Copy the buffer into a byte array
        return data // Return the byte array
    }

    @Throws(IOException::class)
    private fun loadLabelList(context: Context): MutableList<String> {
        val labelList = ArrayList<String>()
        val reader = BufferedReader(InputStreamReader(context.assets.open(LABEL_PATH)))

        var line = reader.readLine()
        while (line != null) {
            labelList.add(line)
            line = reader.readLine()
        }

        reader.close()
        return labelList
    }
}