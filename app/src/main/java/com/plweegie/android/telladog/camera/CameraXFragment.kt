package com.plweegie.android.telladog.camera

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.LifecycleCameraController
import androidx.core.content.ContextCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.mlkit.vision.label.custom.CustomImageLabelerOptions
import com.plweegie.android.telladog.MainActivity
import com.plweegie.android.telladog.MyApp
import com.plweegie.android.telladog.R
import com.plweegie.android.telladog.adapters.InferenceAdapter
import com.plweegie.android.telladog.classifier.DogAnalyzer
import com.plweegie.android.telladog.classifier.DogClassifierViewModel
import com.plweegie.android.telladog.data.DogPrediction
import com.plweegie.android.telladog.data.PredictionRepository
import com.plweegie.android.telladog.databinding.FragmentCameraBinding
import com.plweegie.android.telladog.ui.FragmentSwitchListener
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject
import androidx.core.content.edit


class CameraXFragment : Fragment() {

    companion object {
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
        private const val TAG = "Camera"

        @JvmStatic
        fun newInstance() = CameraXFragment()
    }

    @Inject
    lateinit var imageLabelerOptions: CustomImageLabelerOptions

    @Inject
    lateinit var predictionRepository: PredictionRepository

    private var imageUrl = ""
    private var displayId: Int = -1
    private var cameraController: LifecycleCameraController? = null

    private lateinit var cameraExecutor: ExecutorService

    private var topPrediction: Pair<String, Float>? = null

    private lateinit var inferenceAdapter: InferenceAdapter
    private lateinit var binding: FragmentCameraBinding
    private lateinit var fragmentSwitchListener: FragmentSwitchListener
    private val viewModel: DogClassifierViewModel by viewModels()

    private val activityResultLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val permissionGranted = permissions
            .filter { it.key in REQUIRED_PERMISSIONS }
            .containsValue(true)

        if (permissionGranted) {
            setUpCamera()
        } else {
            showPermissionsInfoDialog()
        }
    }

    override fun onAttach(context: Context) {
        (activity?.application as MyApp).myAppComponent.inject(this)
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

        cameraExecutor.shutdown()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val menuHost: MenuHost = requireActivity()

        menuHost.addMenuProvider(object: MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.fragment_camera, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.change_to_list -> {
                        fragmentSwitchListener.onDogListFragmentSelect()
                        true
                    }
                    R.id.save_pic_data -> {
                        binding.savingProgressBar.isVisible = true
                        takePicture()
                        true
                    }
                    else -> true
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)

        cameraExecutor = Executors.newSingleThreadExecutor()

        binding.inferenceRv.apply {
            layoutManager = LinearLayoutManager(activity)
            setHasFixedSize(true)
            adapter = inferenceAdapter
        }

        binding.text.setOnClickListener {
            binding.inferenceRv.isVisible = true
        }

        // Request camera permissions
        if (allPermissionsGranted()) {
            // Wait for the views to be properly laid out
            binding.cameraPreview.post {
                // Keep track of the display in which this view is attached
                displayId = binding.cameraPreview.display?.displayId ?: -1

                // Set up the camera and its use cases
                setUpCamera()
            }
        } else {
            requestCameraPermission()
        }
    }

    private fun requestCameraPermission() {
        activityResultLauncher.launch(REQUIRED_PERMISSIONS)
    }

    private fun showPermissionsInfoDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.request_permission_title)
            .setMessage(R.string.request_permission)
            .setPositiveButton(R.string.dialog_yes) { _, _ -> requestCameraPermission() }
            .setNegativeButton(R.string.dialog_no) { _, _ -> }
            .create()
            .show()
    }

    private fun setUpCamera() {
        binding.cameraPreview.setOnClickListener {
            binding.inferenceRv.isGone = true
        }

        cameraController = LifecycleCameraController(requireContext()).apply {
            imageAnalysisBackpressureStrategy = ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
            imageCaptureIoExecutor = cameraExecutor

            setImageAnalysisAnalyzer(
                cameraExecutor,
                DogAnalyzer(
                    requireContext(),
                    viewModel.results,
                    imageLabelerOptions,
                    lifecycle
                )
            )

            bindToLifecycle(viewLifecycleOwner)
        }

        binding.cameraPreview.controller = cameraController

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.results.collect {
                    if (it.isEmpty()) {
                        showText(getString(R.string.no_dogs_here))
                    } else {
                        showText(it.first().first)
                    }

                    topPrediction = it.firstOrNull()

                    updateAdapterAsync(it)
                }
            }
        }
    }

    private fun takePicture() {
        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(createImageFile())
            .build()

        val sensorRotation = cameraController?.cameraInfo?.sensorRotationDegrees ?: 0

        PreferenceManager.getDefaultSharedPreferences(requireActivity())
            .edit() {
                putInt(MainActivity.ORIENTATION_PREFERENCE, sensorRotation)
            }

        if (topPrediction == null) return

        cameraController?.takePicture(outputFileOptions, cameraExecutor, object : ImageCapture.OnImageSavedCallback {
            override fun onError(exception: ImageCaptureException) {
                Log.e(TAG, "Error saving image")
            }

            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                val predictionToSave = DogPrediction(
                    topPrediction?.first,
                    topPrediction?.second,
                    imageUrl,
                    Date().time
                )

                predictionRepository.add(predictionToSave)

                activity?.runOnUiThread {
                    binding.savingProgressBar.isVisible = false
                    fragmentSwitchListener.onDogListFragmentSelect()
                }
            }
        })
    }

    private fun createImageFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.UK).format(Date())
        val filename = "PREDICTION_${timestamp}_"
        val storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        val image = File(storageDir, filename)
        imageUrl = image.absolutePath

        return image
    }

    private fun showText(textToShow: String) {
        activity?.runOnUiThread {
            binding.text.text = textToShow
        }
    }

    private fun updateAdapterAsync(predictions: List<Pair<String, Float>>) {
        activity?.runOnUiThread {
            inferenceAdapter.setPredictions(predictions)
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }
}