package net.engawapg.app.camrepo.page

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import net.engawapg.app.camrepo.model.ImageInfo
import net.engawapg.app.camrepo.model.NoteListModel
import net.engawapg.app.camrepo.model.NoteModel

class PageViewModel(app: Application, private val noteModel: NoteModel,
                    private val noteListModel: NoteListModel, private val pageIndex: Int,
                    private val columnCount: Int)
    : AndroidViewModel(app) {

    var modified = false

    fun getItemCount(): Int {
        /* 写真の数に +1(Add_Photoの分) して、列数を求める */
        val photoRow = (noteModel.getPhotoCount(pageIndex) / columnCount) + 1
        return photoRow * columnCount + 2 /* Title, Memo */
    }

    fun getViewType(position: Int) :Int {
        val photoCount =noteModel.getPhotoCount(pageIndex)
        val itemCount = getItemCount()
        return when {
            position == 0 -> VIEW_TYPE_PAGE_TITLE
            position <= photoCount -> VIEW_TYPE_PHOTO
            position == photoCount + 1 -> VIEW_TYPE_ADD_PHOTO
            position == itemCount - 1 -> VIEW_TYPE_MEMO
            else -> VIEW_TYPE_BLANK
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

    fun getPhotoAt(index: Int): ImageInfo? = noteModel.getPhotoAt(pageIndex, index)

    fun movePhoto(from: Int, to: Int) {
        noteModel.movePhoto(pageIndex, from, to)
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