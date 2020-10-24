package net.engawapg.app.camrepo.page

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import net.engawapg.app.camrepo.model.NoteListModel
import net.engawapg.app.camrepo.model.NoteModel

class PageViewModel(app: Application, private val noteModel: NoteModel,
                    private val noteListModel: NoteListModel, private val pageIndex: Int,
                    private val columnCount: Int)
    : AndroidViewModel(app) {

    fun getItemCount(): Int {
        /* 写真の数に +1(Add_Photoの分) して、列数を求める */
        val photoRow = ((noteModel.getPhotoCount(pageIndex) + 1) / columnCount) + 1
        return photoRow * columnCount * columnCount + 2 /* Title, Memo */
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

    fun getMemo() = noteModel.getMemo(pageIndex)

    fun save(){

    }

    companion object {
//        private const val TAG = "PageViewModel"

        const val VIEW_TYPE_PAGE_TITLE = 1
        const val VIEW_TYPE_PHOTO = 2
        const val VIEW_TYPE_MEMO = 3
        const val VIEW_TYPE_ADD_PHOTO = 4
        const val VIEW_TYPE_BLANK = 5
    }
}