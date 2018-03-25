/* Copyright 2018 Jan K Szymanski. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

==============================================================================*/

package com.plweegie.android.telladog.ui

import android.app.Activity
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.provider.MediaStore
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.support.v4.content.ContextCompat
import android.support.v4.content.FileProvider
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.*
import com.plweegie.android.telladog.ImageClassifier
import com.plweegie.android.telladog.MainActivity
import com.plweegie.android.telladog.MyApp
import com.plweegie.android.telladog.R
import com.plweegie.android.telladog.adapters.PhotoGridAdapter
import com.plweegie.android.telladog.data.DogPrediction
import com.plweegie.android.telladog.data.PredictionRepository
import com.plweegie.android.telladog.viewmodels.PredictionListViewModel
import com.plweegie.android.telladog.viewmodels.PredictionListViewModelFactory
import kotlinx.android.synthetic.main.fragment_dog_list.*
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject


class DogListFragment : Fragment() {

    @Inject
    lateinit var mRepository: PredictionRepository

    @Inject
    lateinit var mViewModelFactory: PredictionListViewModelFactory

    private lateinit var mViewModel: PredictionListViewModel
    private lateinit var mClassifier: ImageClassifier
    private lateinit var mAdapter: PhotoGridAdapter
    private lateinit var mFragmentSwitchListener: FragmentSwitchListener

    private var mClassifierThread: HandlerThread? = null
    private var mClassifierHandler: Handler? = null

    private var mCheckedPermissions = false

    override fun onAttach(context: Context?) {
        (activity?.application as MyApp).mAppComponent.inject(this)
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        mViewModel = ViewModelProviders.of(activity as FragmentActivity, mViewModelFactory)
                .get(PredictionListViewModel::class.java)

        mFragmentSwitchListener = activity as MainActivity

        mAdapter = PhotoGridAdapter()
        mAdapter.setHasStableIds(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_dog_list, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val layoutManager = GridLayoutManager(activity, 2)
        predictions_list.layoutManager = layoutManager
        predictions_list.setHasFixedSize(true)
        predictions_list.adapter = mAdapter

        mViewModel.getPredictionList().observe(this, Observer {
            if (it != null) {
                mAdapter.setContent(it)
            }
        })

        take_photo_fab.setOnClickListener {
            if (!mCheckedPermissions && !allPermissionsGranted()) {
                requestPermissions(getRequiredPermissions(), REQUEST_PERMISSIONS)
                return@setOnClickListener
            } else {
                mCheckedPermissions = true
            }
            startCameraIntent()
        }

        setHasOptionsMenu(true)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        try {
            mClassifier = ImageClassifier(activity as Activity)
        } catch (e: IOException) {
            Log.e(TAG, "Failed to initialize an image classifier.")
        }
    }

    override fun onPause() {
        stopBackgroundThread()
        super.onPause()
    }

    override fun onDestroy() {
        mClassifier.close()
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        startBackgroundThread()
        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            if (resultCode == Activity.RESULT_OK) {
                mClassifierHandler?.post {
                    classify(mClassifier.imgUrl)
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_IMAGE_CAPTURE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    startCameraIntent()
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater?.inflate(R.menu.fragment_dog_list, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.change_to_camera -> {
                mFragmentSwitchListener.onCameraFragmentSelect()
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    private fun startBackgroundThread() {
        mClassifierThread = HandlerThread(THREAD_NAME)
        mClassifierThread?.start()
        mClassifierHandler = Handler(mClassifierThread?.looper)
    }

    private fun stopBackgroundThread() {
        mClassifierThread?.quitSafely()
        try {
            mClassifierThread?.join()
            mClassifierThread = null
            mClassifierHandler = null
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    private fun startCameraIntent() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(activity?.packageManager) != null) {

            var photoFile: File? = null
            try {
                photoFile = createImageFile()
            } catch(e: IOException) {
                Log.e(TAG, "Error creating image file")
            }

            if (photoFile != null) {
                val photoURI = FileProvider.getUriForFile(activity as Context,
                        "com.plweegie.android.telladog.fileprovider", photoFile)
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
            }
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val fileName = "PREDICTION_${timestamp}_"
        val storageDir = context?.getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        val image = File.createTempFile(fileName, ".jpg", storageDir)
        mClassifier.imgUrl = image.absolutePath
        return image
    }

    private fun classify(bitmapUrl: String) {
        val bmOptions = BitmapFactory.Options()
        val bitmap = BitmapFactory.decodeFile(bitmapUrl, bmOptions)

        val croppedBitmap = Bitmap.createBitmap(bitmap,
                (bitmap.width - ImageClassifier.DIM_IMG_SIZE_X) / 2,
                (bitmap.height - ImageClassifier.DIM_IMG_SIZE_Y) / 2,
                ImageClassifier.DIM_IMG_SIZE_X,
                ImageClassifier.DIM_IMG_SIZE_Y)

        val predictions = mClassifier.getPredictions(croppedBitmap)
        val dogPrediction = DogPrediction(
                predictions?.get(0)?.first,
                predictions?.get(0)?.second,
                mClassifier.imgUrl,
                Date().time
        )

        activity?.runOnUiThread {
            mRepository.add(dogPrediction)
        }

    }

    private fun getRequiredPermissions(): Array<String> {
        val packageInfo = activity?.packageManager?.getPackageInfo(activity?.packageName,
                PackageManager.GET_PERMISSIONS)
        val ps = packageInfo?.requestedPermissions

        return ps ?: arrayOf()
    }

    private fun allPermissionsGranted(): Boolean {
        for (perm in getRequiredPermissions()) {
            if (ContextCompat.checkSelfPermission(activity as Context, perm) !=
                            PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    companion object {
        const val REQUEST_IMAGE_CAPTURE = 111
        const val REQUEST_PERMISSIONS = 121
        const val THREAD_NAME = "classifier_thread"
        const val TAG = "DogListFragment"
        const val CAMERA_WIDTH_ARG = "camera_width"
        const val CAMERA_HEIGHT_ARG = "camera_height"

        fun newInstance(): DogListFragment = DogListFragment()
    }
}