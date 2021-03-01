package net.engawapg.app.camrepo.page

import android.content.ContentResolver
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.Image
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import java.io.*

class ImageSaver(
    private val image: Image,
    private val dirname: String,
    private val filename: String,
    private val resolver: ContentResolver,
    private val onImageSavedCallback: (Uri?) -> Unit
) : Runnable {
    override fun run() {
        /* get byte data of original image */
        Log.d(TAG, "image size = ${image.width}, ${image.height}")
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)

        /* check if the image is rotated */
        val inputStream = ByteArrayInputStream(bytes)
        val exif = ExifInterface(inputStream)
        val orientation = exif.rotationDegrees
        Log.d(TAG, "orientation = $orientation")

        /* get square cropped bitmap image */
        val croppedBmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size).let {
            val w = it.width
            val h = it.height
            val len = if (w < h) w else h

            val matrix = Matrix()
            if (orientation != 0) {
                matrix.setRotate(orientation.toFloat(), len / 2f, len / 2f)
            }

            Bitmap.createBitmap(it, (w - len) / 2, (h - len) / 2, len, len, matrix, false)
        }

        /* get byte data of cropped image */
        val croppedBytes = ByteArrayOutputStream().apply {
            croppedBmp.compress(Bitmap.CompressFormat.JPEG, 100, this)
        }.toByteArray()

        val values = ContentValues()

        val item = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val folder = File(path, dirname)
            if (!folder.exists()) {
                val result = folder.mkdirs()
                Log.d(TAG, "mkdirs result = $result")
            }

            val file = File(folder, filename)
            val fos = FileOutputStream(file)
            writeBytesToStream(croppedBytes, fos)

            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpg")
            values.put(MediaStore.Images.Media.DATA, file.absolutePath)

            /* return item's uri */
            resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        }
        else {
            values.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/${dirname}/")
            values.put(MediaStore.MediaColumns.IS_PENDING, 1)

            val item = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)!!
            val os = resolver.openOutputStream(item)
            if (os != null) {
                writeBytesToStream(croppedBytes, os)
            }

            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(item, values, null, null)

            /* return item's uri */
            item
        }

        image.close()
        Log.d(TAG, "save image at ${dirname}/${filename}, URI = $item")

        onImageSavedCallback(item)
    }

    private fun writeBytesToStream(bytes: ByteArray, os: OutputStream) {
        try {
            os.write(bytes)
        } catch (e: IOException) {
            Log.e(CameraFragment.TAG, e.toString())
        } finally {
            try {
                os.close()
            } catch (e: IOException) {
                Log.e(CameraFragment.TAG, e.toString())
            }
        }
    }

    companion object {
        const val TAG = "ImageSaver"
    }
}