package net.engawapg.app.camrepo.page

import android.app.Application
import android.content.ContentResolver
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import net.engawapg.app.camrepo.model.ImageInfo
import net.engawapg.app.camrepo.model.NoteListModel
import net.engawapg.app.camrepo.model.NoteModel

class PageViewModel(app: Application, private val noteModel: NoteModel,
                    private val noteListModel: NoteListModel, val pageIndex: Int,
                    private val columnCount: Int)
    : AndroidViewModel(app) {

    var modified = false
    private var photoSelection: MutableList<Boolean>? = null

    fun getItemCount(photoSelectMode: Boolean): Int {
        var n = noteModel.getPhotoCount(pageIndex)
        if (!photoSelectMode) n += 1 /* Add Photoの分 */
        n += columnCount - (n % columnCount) /* Blankの分 */
        if (!photoSelectMode) n += 2 /* Title, Memoの分 */
        Log.d(TAG, "ItemCount = $n")
        return n
    }

    fun getViewType(position: Int, photoSelectMode: Boolean) :Int {
        val photoCount =noteModel.getPhotoCount(pageIndex)
        return if (photoSelectMode) {
            when {
                position < photoCount -> VIEW_TYPE_PHOTO
                else -> VIEW_TYPE_BLANK
            }
        } else {
            val itemCount = getItemCount(photoSelectMode)
            when {
                position == 0 -> VIEW_TYPE_PAGE_TITLE
                position <= photoCount -> VIEW_TYPE_PHOTO
                position == photoCount + 1 -> VIEW_TYPE_ADD_PHOTO
                position == itemCount - 1 -> VIEW_TYPE_MEMO
                else -> VIEW_TYPE_BLANK
            }
        }
    }

    fun getPageTitle() = noteModel.getTitle(pageIndex)
    fun setPageTitle(title: String) {
        val oldTitle = noteModel.getTitle(pageIndex)
        if (title != oldTitle) {
            noteModel.setTitle(pageIndex, title)
            modified = true
        }
    }

    fun getMemo() = noteModel.getMemo(pageIndex)
    fun setMemo(memo: String) {
        val oldMemo = noteModel.getMemo(pageIndex)
        if (memo != oldMemo) {
            noteModel.setMemo(pageIndex, memo)
            modified = true
        }
    }

    fun getPhotoIndexOfItemIndex(itemIndex: Int, editMode: Boolean): Int {
        return if (editMode) itemIndex else itemIndex - 1
    }

    private fun getPhotoAt(index: Int): ImageInfo? = noteModel.getPhotoAt(pageIndex, index)

    fun getPhotoBitmap(index: Int, resolver: ContentResolver): Bitmap? {
        val imageInfo = getPhotoAt(index)
        return imageInfo?.getBitmapThumbnailWithResolver(resolver)
    }

    fun movePhoto(from: Int, to: Int) {
        noteModel.movePhoto(pageIndex, from, to)
        modified = true
    }

    fun initPhotoSelection() {
        photoSelection  = MutableList(noteModel.getPhotoCount(pageIndex)){false}
    }

    fun setPhotoSelection(index: Int, sel: Boolean) {
        photoSelection?.let {
            if (index < it.size) {
                it[index] = sel
            }
        }
    }

    fun getPhotoSelection(index: Int) = photoSelection?.getOrNull(index) ?: false

    fun isPhotoSelected() = photoSelection?.contains(true) ?: false

    fun deleteSelectedPhotos() {
        val indexes = mutableListOf<Int>()
        photoSelection?.forEachIndexed{ index, b -> if (b) indexes.add(index) }
        Log.d(TAG, "Delete at $indexes")
        noteModel.deletePhotosAt(pageIndex, indexes)
        modified = true
    }

    fun save(){
        if (modified) {
            Log.d(TAG, "Note modified")
            modified = false
            noteModel.save()
            noteListModel.updateLastModifiedDate(noteModel.fileName)
            noteListModel.save()
        }
    }

    companion object {
        private const val TAG = "PageViewModel"

        const val VIEW_TYPE_PAGE_TITLE = 1
        const val VIEW_TYPE_PHOTO = 2
        const val VIEW_TYPE_MEMO = 3
        const val VIEW_TYPE_ADD_PHOTO = 4
        const val VIEW_TYPE_BLANK = 5
    }
}