package com.smedialink.storage.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.util.Base64
import java.io.ByteArrayOutputStream

class AvatarSerializer {

    companion object {

        @JvmStatic
        fun getStringFromBitmap(pictureBitmap: Bitmap, size: Int): String {
            val roundedBitmap = getCircledBitmap(pictureBitmap)
            val scaledBitmap = Bitmap.createScaledBitmap(roundedBitmap, size, size, false)
            val compressionQuality = 75
            val encodedImage: String
            val byteArrayBitmapStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.PNG, compressionQuality, byteArrayBitmapStream)

            val babs = byteArrayBitmapStream.toByteArray()
            encodedImage = Base64.encodeToString(babs, Base64.DEFAULT)
            return encodedImage
        }

        @JvmStatic
        fun getBitmapFromString(pictureString: String): Bitmap {
            val decodedString = Base64.decode(pictureString, Base64.DEFAULT)
            return BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
        }

        private fun getCircledBitmap(bitmap: Bitmap): Bitmap {
            val output: Bitmap = if (bitmap.width > bitmap.height) {
                Bitmap.createBitmap(bitmap.height, bitmap.height, Bitmap.Config.ARGB_8888)
            } else {
                Bitmap.createBitmap(bitmap.width, bitmap.width, Bitmap.Config.ARGB_8888)
            }

            val canvas = Canvas(output)

            val color = -0xbdbdbe
            val paint = Paint()
            val rect = Rect(0, 0, bitmap.width, bitmap.height)

            val r = if (bitmap.width > bitmap.height) {
                bitmap.height / 2f
            } else {
                bitmap.width / 2f
            }

            paint.isAntiAlias = true
            canvas.drawARGB(0, 0, 0, 0)
            paint.color = color
            canvas.drawCircle(r, r, r, paint)
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            canvas.drawBitmap(bitmap, rect, rect, paint)
            return output
        }
    }
}
