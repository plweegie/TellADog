package com.plweegie.android.telladog.async

import android.content.Context
import android.media.Image
import android.os.AsyncTask
import com.plweegie.android.telladog.ui.FragmentSwitchListener
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


class ImageSaverTask(private val mFile: File, private val mListener: FragmentSwitchListener)
        : AsyncTask<Image, Int, Unit>() {

    override fun doInBackground(vararg images: Image?) {
        val buffer = images[0]!!.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        var output: FileOutputStream? = null

        try {
            output = FileOutputStream(mFile)
            output.write(bytes)
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            images[0]?.close()

            if (output != null) {
                try {
                    output.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onProgressUpdate(vararg values: Int?) {

    }
}