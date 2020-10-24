package net.engawapg.app.camrepo.page

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import net.engawapg.app.camrepo.model.NoteListModel
import net.engawapg.app.camrepo.model.NoteModel

class PageViewModel(app: Application, private val noteModel: NoteModel,
                    private val noteListModel: NoteListModel, private val pageIndex: Int)
    : AndroidViewModel(app) {

    fun getItemCount() = noteModel.getPhotoCount(pageIndex) + 3 /* Title, Memo, Add_Photo */

    fun getViewType(position: Int) :Int {
        val count = getItemCount()
        return when (position) {
            0 -> VIEW_TYPE_PAGE_TITLE
            count - 1 -> VIEW_TYPE_MEMO
            count - 2 -> VIEW_TYPE_ADD_PHOTO
            else -> VIEW_TYPE_PHOTO
        }
    }

    fun getPageTitle() = noteModel.getTitle(pageIndex)

    fun getMemo() = noteModel.getMemo(pageIndex)

    fun save(){

    }

    companion object {
        private const val TAG = "PageViewModel"

        const val VIEW_TYPE_PAGE_TITLE = 1
        const val VIEW_TYPE_PHOTO = 2
        const val VIEW_TYPE_MEMO = 3
        const val VIEW_TYPE_ADD_PHOTO = 4
    }
}