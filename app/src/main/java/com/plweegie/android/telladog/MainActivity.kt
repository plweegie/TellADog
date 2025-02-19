package com.plweegie.android.telladog

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.plweegie.android.telladog.camera.CameraXFragment
import com.plweegie.android.telladog.databinding.ActivityMainBinding
import com.plweegie.android.telladog.ui.DogListFragment
import com.plweegie.android.telladog.ui.FragmentSwitchListener
import com.plweegie.android.telladog.viewmodels.ModelDownloadViewModel
import com.plweegie.android.telladog.viewmodels.ModelDownloadViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class MainActivity : AppCompatActivity(), FragmentSwitchListener {

    private lateinit var binding: ActivityMainBinding

    private var currentUser: FirebaseUser? = null

    @Inject
    lateinit var viewModelFactory: ModelDownloadViewModelFactory

    private val viewModel: ModelDownloadViewModel by viewModels { viewModelFactory }

    private val startForResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            currentUser = FirebaseAuth.getInstance().currentUser
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        (application as MyApp).myAppComponent.inject(this)
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)

        window.apply {
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            statusBarColor = ContextCompat.getColor(this@MainActivity, R.color.colorToolbar)
        }

        setContentView(binding.root)
        setSupportActionBar(binding.mainToolbar)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            ViewCompat.setOnApplyWindowInsetsListener(binding.mainToolbar) { v, insets ->
                val viewInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars())

                v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    topMargin = viewInsets.top
                }

                WindowInsetsCompat.CONSUMED
            }
        }

        currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            startAuth()
        }

        var fragment = supportFragmentManager.findFragmentById(R.id.container)

        if (fragment == null) {
            fragment = DogListFragment.newInstance(currentUser?.uid)
            supportFragmentManager.beginTransaction()
                    .replace(R.id.container, fragment)
                    .commit()
        }

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.modelDownloadState.collect {
                    when (it) {
                        ModelDownloadViewModel.DownloadState.IN_PROGRESS -> {
                            showModelDownloadProgress()
                        }
                        ModelDownloadViewModel.DownloadState.COMPLETE -> {
                            showCamera()
                        }
                        ModelDownloadViewModel.DownloadState.IDLE -> {}
                    }
                }
            }
        }
    }

    override fun onDogListFragmentSelect() {
        val dogListFragment = DogListFragment.newInstance(currentUser?.uid)
        supportFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.fragment_slide_in, R.anim.fragment_slide_out)
                .replace(R.id.container, dogListFragment)
                .commit()
    }

    override fun onCameraFragmentSelect() {
        lifecycleScope.launch(Dispatchers.IO) {
            if (viewModel.isRemoteClassifierModelDownloadedAsync().await()) {
                withContext(Dispatchers.Main) {
                    showCamera()
                }
            } else {
                viewModel.downloadRemoteClassifierModel()
            }
        }
    }

    private fun showCamera() {
        val cameraFragment = CameraXFragment.newInstance()
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.camera_slide_in, R.anim.camera_slide_out)
            .replace(R.id.container, cameraFragment)
            .commit()
    }

    private fun showModelDownloadProgress() {
        Toast.makeText(this, R.string.wait_download, Toast.LENGTH_LONG).show()
    }

    private fun startAuth() {
        val providers = arrayListOf(AuthUI.IdpConfig.GoogleBuilder().build())

        startForResult.launch(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .build()
        )
    }

    companion object {
        const val ORIENTATION_PREFERENCE = "orientation_pref"
    }
}
