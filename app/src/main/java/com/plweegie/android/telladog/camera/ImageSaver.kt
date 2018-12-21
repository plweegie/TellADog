package com.plweegie.android.telladog.camera

import android.media.Image
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


class ImageSaver(private val file: File, private val image: Image) : Runnable {

    interface ImageSaverListener {
        fun onImageSaved()
    }

    var listener: ImageSaverListener? = null

    override fun run() {
        val byteBuffer = image.planes[0].buffer
        val bytes = ByteArray(byteBuffer.remaining())
        byteBuffer.get(bytes)
        var output: FileOutputStream? = null

        try {
            output = FileOutputStream(file)
            output.write(bytes)
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            image.close()

            output?.run {
                try {
                    close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }

            listener?.onImageSaved()
        }
    }
}