package net.engawapg.app.camrepo.model

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ImageInfo(val uri: Uri) {

    suspend fun getBitmapWithResolver(resolver: ContentResolver): Bitmap {
        return withContext(Dispatchers.IO) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                MediaStore.Images.Media.getBitmap(resolver, uri)
            } else {
                val decoder = ImageDecoder.createSource(resolver, uri)
                ImageDecoder.decodeBitmap(decoder)
            }
        }
    }
}