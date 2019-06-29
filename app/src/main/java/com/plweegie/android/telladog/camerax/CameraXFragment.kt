package com.plweegie.android.telladog.camerax

import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.DisplayMetrics
import android.util.Rational
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.*
import androidx.fragment.app.Fragment
import com.plweegie.android.telladog.MyApp
import com.plweegie.android.telladog.R
import kotlinx.android.synthetic.main.fragment_camera.*
import javax.inject.Inject


class CameraXFragment: Fragment() {

    private var displayId = -1
    private var lensFacing = CameraX.LensFacing.BACK

    private var imageAnalyzer: ImageAnalysis? = null
    private var preview: Preview? = null

    @Inject
    lateinit var firebaseAnalyzer: ImageClassifierAnalyzer

    /** Internal reference of the [DisplayManager] */
    private lateinit var displayManager: DisplayManager
    /**
     * We need a display listener for orientation changes that do not trigger a configuration
     * change, for example if we choose to override config change in manifest or for 180-degree
     * orientation changes.
     */
    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit
        override fun onDisplayChanged(displayId: Int) = view?.let { view ->
            if (displayId == this@CameraXFragment.displayId) {
                preview?.setTargetRotation(view.display.rotation)
                imageAnalyzer?.setTargetRotation(view.display.rotation)
            }
        } ?: Unit
    }

    override fun onAttach(context: Context?) {
        (activity?.application as MyApp).mAppComponent.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_camera, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Every time the orientation of device changes, recompute layout
        displayManager = view_finder.context
                .getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        displayManager.registerDisplayListener(displayListener, null)
        view_finder?.post {
            displayId = view_finder.display.displayId
            bindCameraUseCases()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        displayManager.unregisterDisplayListener(displayListener)
    }

    private fun bindCameraUseCases() {

        CameraX.unbindAll()

        val metrics = DisplayMetrics().also { view_finder.display.getRealMetrics(it) }
        val screenAspectRatio = Rational(metrics.widthPixels, metrics.heightPixels)

        // Set up the view finder use case to display camera preview
        val viewFinderConfig = PreviewConfig.Builder().apply {
            setLensFacing(lensFacing)
            // We also provide an aspect ratio in case the exact resolution is not available
            setTargetAspectRatio(screenAspectRatio)
            setTargetRotation(view_finder.display.rotation)
        }.build()

        preview = AutoFitPreviewBuilder.build(viewFinderConfig, view_finder)

        val analyzerConfig = ImageAnalysisConfig.Builder().apply {
            setLensFacing(lensFacing)

            val analyzerThread = HandlerThread("Classifier").apply { start() }
            setCallbackHandler(Handler(analyzerThread.looper))
            setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)

            setTargetResolution(Size(224, 224))
            setTargetRotation(view_finder.display.rotation)
        }.build()

        imageAnalyzer = ImageAnalysis(analyzerConfig).apply {
            analyzer = firebaseAnalyzer.apply {
                onFrameAnalyzed { predictions ->
                    val size = predictions.size
                }
            }
        }

        CameraX.bindToLifecycle(this, preview, imageAnalyzer)
    }
}