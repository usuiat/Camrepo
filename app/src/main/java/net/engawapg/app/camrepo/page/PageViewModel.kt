package net.engawapg.app.camrepo.page

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import net.engawapg.app.camrepo.model.NoteListModel
import net.engawapg.app.camrepo.model.NoteModel

class PageViewModel(app: Application, private val noteModel: NoteModel,
                    private val noteListModel: NoteListModel): AndroidViewModel(app) {

    fun getItemCount() = list.size

    fun getViewType(position: Int) = list[position]

    fun getPageTitle() = "Title"

    fun getMemo() = "MEMOMOMOMMOMOMOMOOOOMOM"

    fun save(){

    }

    companion object {
        const val VIEW_TYPE_PAGE_TITLE = 1
        const val VIEW_TYPE_PHOTO = 2
        const val VIEW_TYPE_MEMO = 3
        const val VIEW_TYPE_ADD_PHOTO = 4

        private val list = listOf(
            VIEW_TYPE_PAGE_TITLE,
            VIEW_TYPE_PHOTO,
            VIEW_TYPE_PHOTO,
            VIEW_TYPE_PHOTO,
            VIEW_TYPE_PHOTO,
            VIEW_TYPE_PHOTO,
            VIEW_TYPE_PHOTO,
            VIEW_TYPE_ADD_PHOTO,
            VIEW_TYPE_MEMO
        )
    }
}