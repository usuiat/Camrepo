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
    fun setNoteTitle(title: String) {
        noteModel.title = title
    }
}