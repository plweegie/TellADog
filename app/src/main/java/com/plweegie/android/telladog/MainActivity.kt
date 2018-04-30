package com.plweegie.android.telladog

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.plweegie.android.telladog.camera.CameraFragment
import com.plweegie.android.telladog.ui.DogListFragment
import com.plweegie.android.telladog.ui.FragmentSwitchListener

class MainActivity : AppCompatActivity(), FragmentSwitchListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        var fragment = supportFragmentManager.findFragmentById(R.id.container)

        if (fragment == null) {
            fragment = CameraFragment.newInstance()
            supportFragmentManager.beginTransaction()
                    .replace(R.id.container, fragment)
                    .commit()
        }
    }

    override fun onDogListFragmentSelect() {
        val dogListFragment = DogListFragment.newInstance()
        supportFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.fragment_slide_in, R.anim.fragment_slide_out)
                .replace(R.id.container, dogListFragment)
                .commit()
    }

    override fun onCameraFragmentSelect() {
        val cameraFragment = CameraFragment.newInstance()
        supportFragmentManager.beginTransaction()
                .replace(R.id.container, cameraFragment)
                .commit()
    }

    companion object {

        // Used to load the 'native-lib' library on application startup.
        init {
            System.loadLibrary("native-lib")
        }
    }
}
