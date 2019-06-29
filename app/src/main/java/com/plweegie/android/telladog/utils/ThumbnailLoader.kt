package com.plweegie.android.telladog.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.camera.core.ImageProxy


class ThumbnailLoader {

    companion object {

        private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {

            val height = options.outHeight
            val width = options.outWidth
            var inSampleSize = 1

            if (height > reqHeight || width > reqWidth) {
                val halfHeight = height / 2
                val halfWidth = width / 2

                while ((halfHeight / inSampleSize) >= reqHeight &&
                        (halfWidth / inSampleSize) >= reqWidth) {
                    inSampleSize *= 2
                }
            }

            return inSampleSize
        }

        fun decodeBitmapFromFile(fileAbsolutePath: String?, reqWidth: Int, reqHeight: Int): Bitmap? {

            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true

            return if (fileAbsolutePath != null) {
                BitmapFactory.decodeFile(fileAbsolutePath, options)
                options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
                options.inJustDecodeBounds = false
                BitmapFactory.decodeFile(fileAbsolutePath, options)
            } else {
                null
            }
        }

        fun decodeBitmapFromImageProxy(imageProxy: ImageProxy, reqWidth: Int = 224, reqHeight: Int = 224): Bitmap? {
            val pixelBuffer = imageProxy.planes[0].buffer
            pixelBuffer.rewind()

            val bytes = ByteArray(pixelBuffer.capacity()).also {
                pixelBuffer.get(it)
            }

            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true

            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
            options.inJustDecodeBounds = false
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        }
    }
}