package com.plweegie.android.telladog.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix


object ThumbnailLoader {

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

    fun decodeBitmapFromFile(fileAbsolutePath: String?,
                             reqWidth: Int, reqHeight: Int, orientation: Int): Bitmap? {

        val matrix = Matrix().apply { postRotate(orientation.toFloat()) }

        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true

        return if (fileAbsolutePath != null) {
            BitmapFactory.decodeFile(fileAbsolutePath, options)
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
            options.inJustDecodeBounds = false

            val bitmap = BitmapFactory.decodeFile(fileAbsolutePath, options)
            return Bitmap.createBitmap(bitmap, 0, 0,
                    bitmap.width, bitmap.height, matrix, true)
        } else {
            null
        }
    }
}