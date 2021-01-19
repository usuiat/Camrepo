package net.engawapg.app.camrepo.notelist

import androidx.lifecycle.ViewModel
import net.engawapg.app.camrepo.model.NoteListModel
import net.engawapg.app.camrepo.model.NoteModel

class EditTitleViewModel(private val noteModel: NoteModel, private val noteListModel: NoteListModel)
    : ViewModel() {
    var title: String = noteModel.title
    var subTitle: String = noteModel.subTitle

    fun save() {
        noteModel.title = title
        noteModel.subTitle = subTitle
        noteModel.save()
        noteListModel.save()
    }
}