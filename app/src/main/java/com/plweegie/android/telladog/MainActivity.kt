package com.plweegie.android.telladog

import android.os.Build
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.plweegie.android.telladog.camera.CameraFragment
import com.plweegie.android.telladog.ui.DogListFragment
import com.plweegie.android.telladog.ui.FragmentSwitchListener
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), FragmentSwitchListener {

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
