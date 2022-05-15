package com.plweegie.android.telladog.classifier

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.widget.Toast
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.custom.CustomImageLabelerOptions
import com.plweegie.android.telladog.utils.CameraXImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext


class DogAnalyzer(
    private val context: Context,
    private val results: MutableStateFlow<List<Pair<String, Float>>>,
    imageLabelerOptions: CustomImageLabelerOptions,
    lifecycle: Lifecycle
) : ImageAnalysis.Analyzer {

    private companion object {
        const val DIM_IMG_SIZE_X = 224
        const val DIM_IMG_SIZE_Y = 224
    }

    private val imageLabeler = ImageLabeling.getClient(imageLabelerOptions)
    private val imageLabelerScope = lifecycle.coroutineScope + Dispatchers.Default

    init {
        lifecycle.addObserver(imageLabeler)
    }

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees

        val imageHeight = imageProxy.height
        val imageWidth = imageProxy.width

        val cropLeft = (imageWidth - DIM_IMG_SIZE_X) / 2
        val cropTop = (imageHeight - DIM_IMG_SIZE_Y) / 2
        val cropRight = (imageWidth + DIM_IMG_SIZE_X) / 2
        val cropBottom = (imageHeight + DIM_IMG_SIZE_Y) / 2
        val cropRect = Rect(cropLeft, cropTop, cropRight, cropBottom)

        imageProxy.image?.let {
            val bitmap = CameraXImageUtils.convertYuv420888ImageToBitmap(it)
            val croppedBitmap = CameraXImageUtils.rotateAndCrop(bitmap, rotationDegrees, cropRect)

            //val inputImage = InputImage.fromMediaImage(it, imageProxy.imageInfo.rotationDegrees)
            val inputImage = InputImage.fromBitmap(croppedBitmap, 0)
            imageLabelerScope.launch {
                processImage(inputImage)
                imageProxy.close()
            }
        }
    }

    private suspend fun processImage(image: InputImage) {
        try {
            imageLabeler.process(image).await().also { output ->
                results.value = output.map { Pair(it.text, it.confidence) }
            }
        } catch (e: Exception) {
            getErrorMessage(e)?.let {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun getErrorMessage(exception: Exception): String? {
        val mlKitException = exception as? MlKitException ?: return exception.message
        return if (mlKitException.errorCode == MlKitException.UNAVAILABLE) {
            "Waiting for text recognition model to be downloaded"
        } else exception.message
    }
}