package net.engawapg.app.camrepo.model

import android.net.Uri
import android.util.Log
import androidx.recyclerview.widget.DiffUtil

class ImageInfo(val uri: Uri) {
    class DiffCallback : DiffUtil.ItemCallback<ImageInfo>() {

        override fun areContentsTheSame(oldItem: ImageInfo, newItem: ImageInfo): Boolean {
            Log.d("ImageInfo.DiffCallback", "oid=$oldItem, new=$newItem")
            return oldItem.uri == newItem.uri
        }

        override fun areItemsTheSame(oldItem: ImageInfo, newItem: ImageInfo): Boolean {
            return oldItem.uri == newItem.uri
        }
    }
}