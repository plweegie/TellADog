package com.plweegie.android.telladog

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.plweegie.android.telladog.camera.CameraFragment
import com.plweegie.android.telladog.ui.DogListFragment
import com.plweegie.android.telladog.ui.FragmentSwitchListener
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), FragmentSwitchListener {

    private var currentUser: FirebaseUser? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.apply {
                decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                statusBarColor = ContextCompat.getColor(this@MainActivity, R.color.colorToolbar)
            }
        }

        setContentView(R.layout.activity_main)
        setSupportActionBar(main_toolbar)

        currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            startAuth()
        }

        var fragment = supportFragmentManager.findFragmentById(R.id.container)

        if (fragment == null) {
            fragment = CameraFragment.newInstance()
            supportFragmentManager.beginTransaction()
                    .replace(R.id.container, fragment)
                    .commit()
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
        val cameraFragment = CameraFragment.newInstance()
        supportFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.camera_slide_in, R.anim.camera_slide_out)
                .replace(R.id.container, cameraFragment)
                .commit()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SIGNIN_REQUEST_CODE) {
            val response = IdpResponse.fromResultIntent(data)

            if (resultCode == Activity.RESULT_OK) {
                currentUser = FirebaseAuth.getInstance().currentUser
            }
        }
    }

    private fun startAuth() {
        val providers = arrayListOf(AuthUI.IdpConfig.GoogleBuilder().build())
        startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(providers)
                        .build(),
                SIGNIN_REQUEST_CODE
        )
    }

    companion object {

        private const val SIGNIN_REQUEST_CODE = 2

        // Used to load the 'native-lib' library on application startup.
        init {
            System.loadLibrary("native-lib")
        }
    }
}
