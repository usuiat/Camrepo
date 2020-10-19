package net.engawapg.app.camrepo.note

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import net.engawapg.app.camrepo.model.NoteListModel
import net.engawapg.app.camrepo.model.NoteModel

class NoteViewModel(app: Application, private val noteModel: NoteModel,
                    private val noteListModel: NoteListModel)
    : AndroidViewModel(app) {

    private var noteIndex: Int? = null

    fun setNoteIndex(index: Int) {
        noteIndex = index
    }

    fun getNoteTitle() = noteModel.title
    fun getNoteSubTitle() = noteModel.subTitle

    fun setNoteTitle(title: String, subTitle: String) {
        noteModel.title = title
        noteModel.subTitle = subTitle
        noteListModel.updateNoteTitle(noteModel.fileName, title, subTitle)
    }

    fun getItemCount() = list.size

    fun getViewType(index: Int): Int {
        return list[index]
    }

    fun save() {
        noteModel.save()
        noteListModel.save()
    }

    companion object {
        const val VIEW_TYPE_TITLE = 1
        const val VIEW_TYPE_PAGE_TITLE = 2
        const val VIEW_TYPE_PHOTO = 3
        const val VIEW_TYPE_MEMO = 4
        const val VIEW_TYPE_BLANK = 5
        const val VIEW_TYPE_ADD_PHOTO = 6
        val list: List<Int> = listOf(
            VIEW_TYPE_TITLE,

            VIEW_TYPE_PAGE_TITLE,
            VIEW_TYPE_PHOTO,
            VIEW_TYPE_ADD_PHOTO,
            VIEW_TYPE_BLANK,
            VIEW_TYPE_BLANK,
            VIEW_TYPE_MEMO,

            VIEW_TYPE_PAGE_TITLE,
            VIEW_TYPE_PHOTO,
            VIEW_TYPE_PHOTO,
            VIEW_TYPE_PHOTO,
            VIEW_TYPE_ADD_PHOTO,
            VIEW_TYPE_MEMO,

            VIEW_TYPE_PAGE_TITLE,
            VIEW_TYPE_PHOTO,
            VIEW_TYPE_PHOTO,
            VIEW_TYPE_PHOTO,
            VIEW_TYPE_PHOTO,
            VIEW_TYPE_ADD_PHOTO,
            VIEW_TYPE_BLANK,
            VIEW_TYPE_BLANK,
            VIEW_TYPE_BLANK,
            VIEW_TYPE_MEMO,

            VIEW_TYPE_PAGE_TITLE,
            VIEW_TYPE_PHOTO,
            VIEW_TYPE_PHOTO,
            VIEW_TYPE_PHOTO,
            VIEW_TYPE_PHOTO,
            VIEW_TYPE_PHOTO,
            VIEW_TYPE_PHOTO,
            VIEW_TYPE_PHOTO,
            VIEW_TYPE_PHOTO,
            VIEW_TYPE_PHOTO,
            VIEW_TYPE_ADD_PHOTO,
            VIEW_TYPE_BLANK,
            VIEW_TYPE_BLANK,
            VIEW_TYPE_MEMO
        )
    }
}