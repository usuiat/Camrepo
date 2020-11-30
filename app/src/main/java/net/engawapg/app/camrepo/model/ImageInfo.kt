package net.engawapg.app.camrepo.model

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.lang.Exception

class ImageInfo(val uri: Uri) {

    suspend fun getBitmapWithResolver(resolver: ContentResolver): Bitmap? {
        return getBitmapWithResolver(resolver, 0)
    }

    suspend fun getBitmapWithResolver(resolver: ContentResolver, size: Int): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                    MediaStore.Images.Media.getBitmap(resolver, uri)
                } else {
                    val decoder = ImageDecoder.createSource(resolver, uri)
                    ImageDecoder.decodeBitmap(decoder) { dec, _, _ ->
                        if (size > 0) dec.setTargetSize(size, size)
                    }
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    fun getBitmapThumbnailWithResolver(resolver: ContentResolver): Bitmap? {
            return if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                val id = uri.lastPathSegment?.toLong() ?: 0
                MediaStore.Images.Thumbnails.getThumbnail(
                    resolver, id,
                    MediaStore.Images.Thumbnails.MINI_KIND, null
                )
            } else {
                try {
                    resolver.loadThumbnail(uri, Size(256, 256), null)
                } catch (e: IOException) {
                    null
                }
            }
    }
}