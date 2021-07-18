package net.engawapg.app.camrepo.page

import android.net.Uri
import androidx.recyclerview.widget.DiffUtil

data class PhotoGalleryItem(
    val uri: Uri
) {
    companion object {
        val DIFF_UTIL = object: DiffUtil.ItemCallback<PhotoGalleryItem>() {
            override fun areContentsTheSame(oldItem: PhotoGalleryItem, newItem: PhotoGalleryItem)
                    : Boolean {
                return oldItem == newItem
            }

            override fun areItemsTheSame(oldItem: PhotoGalleryItem, newItem: PhotoGalleryItem)
                    : Boolean {
                return oldItem.uri == newItem.uri
            }
        }
    }
}