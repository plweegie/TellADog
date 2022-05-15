package com.plweegie.android.telladog.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.mlkit.vision.label.custom.CustomImageLabelerOptions
import com.plweegie.android.telladog.MainActivity
import com.plweegie.android.telladog.MyApp
import com.plweegie.android.telladog.R
import com.plweegie.android.telladog.adapters.InferenceAdapter
import com.plweegie.android.telladog.classifier.DogAnalyzer
import com.plweegie.android.telladog.classifier.DogClassifierViewModel
import com.plweegie.android.telladog.databinding.FragmentCameraBinding
import com.plweegie.android.telladog.ui.FragmentSwitchListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min


class CameraXFragment : Fragment(), ImageSaver.ImageSaverListener {

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
        private const val TAG = "Camera"

        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0

        @JvmStatic
        fun newInstance() = CameraXFragment()
    }

    @Inject
    lateinit var imageLabelerOptions: CustomImageLabelerOptions

    private var displayId: Int = -1
    private var cameraProvider: ProcessCameraProvider? = null
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var fragmentScope: CoroutineScope
    private val fragmentJob = Job()

    private lateinit var inferenceAdapter: InferenceAdapter
    private lateinit var binding: FragmentCameraBinding
    private lateinit var fragmentSwitchListener: FragmentSwitchListener
    private val viewModel: DogClassifierViewModel by viewModels()

    override fun onAttach(context: Context) {
        (activity?.application as MyApp).machineLearningComponent.inject(this)
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        fragmentSwitchListener = activity as MainActivity
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        inferenceAdapter = InferenceAdapter(requireActivity()).apply {
            setHasStableIds(true)
        }

        binding = FragmentCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()

        fragmentJob.cancel()
        cameraExecutor.shutdown()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cameraExecutor = Executors.newSingleThreadExecutor()
        fragmentScope = CoroutineScope(cameraExecutor.asCoroutineDispatcher() + fragmentJob)

        binding.inferenceRv.apply {
            layoutManager = LinearLayoutManager(activity)
            setHasFixedSize(true)
            adapter = inferenceAdapter
        }

        binding.text?.setOnClickListener {
            binding.inferenceRv.isVisible = true
        }

        // Request camera permissions
        if (allPermissionsGranted()) {
            // Wait for the views to be properly laid out
            binding.cameraPreview?.post {
                // Keep track of the display in which this view is attached
                displayId = binding.cameraPreview?.display?.displayId ?: -1

                // Set up the camera and its use cases
                setUpCamera()
            }
        } else {
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_camera, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.change_to_list -> {
                fragmentSwitchListener.onDogListFragmentSelect()
                true
            }
            R.id.save_pic_data -> {
                // TODO take picture
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    private fun setUpCamera() {
        binding.cameraPreview?.setOnClickListener {
            binding.inferenceRv.isGone = true
        }
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({

            // CameraProvider
            cameraProvider = cameraProviderFuture.get()
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun bindCameraUseCases() {
        val localCameraProvider = cameraProvider
            ?: throw IllegalStateException("Camera initialization failed.")

        // Get screen metrics used to setup camera for full screen resolution
        val metrics = DisplayMetrics().also { binding.cameraPreview?.display?.getRealMetrics(it) }
        Log.d(TAG, "Screen metrics: ${metrics.widthPixels} x ${metrics.heightPixels}")

        val screenAspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels)
        Log.d(TAG, "Preview aspect ratio: $screenAspectRatio")

        val rotation = binding.cameraPreview?.display?.rotation ?: 0

        preview = Preview.Builder()
            .setTargetAspectRatio(screenAspectRatio)
            .setTargetRotation(rotation)
            .build()

        imageAnalyzer = ImageAnalysis.Builder()
            .setTargetAspectRatio(screenAspectRatio)
            .setTargetRotation(rotation)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(
                    cameraExecutor,
                    DogAnalyzer(requireContext(), viewModel.results, imageLabelerOptions, lifecycle)
                )
            }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.results.collect {
                    if (it.isEmpty()) {
                        showText(getString(R.string.no_dogs_here))
                    } else {
                        showText(it.first().first)
                    }

                    updateAdapterAsync(it)
                }
            }
        }

        val cameraSelector =
            CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()

        try {
            localCameraProvider.unbindAll()

            camera = localCameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                preview,
                imageAnalyzer
            )
            preview?.setSurfaceProvider(binding.cameraPreview?.surfaceProvider)
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Use case binding failed. This must be running on main thread.", e)
        }
    }

    override fun onImageSaved() {
        activity?.runOnUiThread {
            fragmentSwitchListener.onDogListFragmentSelect()
        }
    }

    private fun showText(textToShow: String) {
        activity?.runOnUiThread {
            binding.text?.text = textToShow
        }
    }

    private fun updateAdapterAsync(predictions: List<Pair<String, Float>>) {
        activity?.runOnUiThread {
            inferenceAdapter.setPredictions(predictions)
        }
    }

    /**
     *  [androidx.camera.core.ImageAnalysisConfig] requires enum value of
     *  [androidx.camera.core.AspectRatio]. Currently it has values of 4:3 & 16:9.
     *
     *  Detecting the most suitable ratio for dimensions provided in @params by comparing absolute
     *  of preview ratio to one of the provided values.
     *
     *  @param width - preview width
     *  @param height - preview height
     *  @return suitable aspect ratio
     */
    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = ln(max(width, height).toDouble() / min(width, height))
        if (abs(previewRatio - ln(RATIO_4_3_VALUE))
            <= abs(previewRatio - ln(RATIO_16_9_VALUE))
        ) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }
}