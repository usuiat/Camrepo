package net.engawapg.app.camrepo.notelist

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import net.engawapg.app.camrepo.Constants
import net.engawapg.app.camrepo.model.NoteListModel
import net.engawapg.app.camrepo.model.NoteModel
import net.engawapg.app.camrepo.model.NoteProperty
import org.koin.core.qualifier.named
import org.koin.java.KoinJavaComponent.getKoin

class NoteListViewModel(app: Application, private val model: NoteListModel)
    : AndroidViewModel(app) {

    fun onClickAdd() {
        Log.d(TAG, "add")
        createNoteModel(model.createNewNote("New Note ${model.list.size + 1}"))
    }

    fun save() {
        model.save()
    }

    fun getItemCount(): Int {
        return model.list.size
    }

    fun getItem(index: Int): NoteProperty {
        return model.list[index]
    }

    fun onClickNoteItem(property: NoteProperty) {
        createNoteModel(property)
    }

    fun deleteItemsAt(indexes: List<Int>) {
        model.deleteNotesAt(indexes)
    }

    private fun createNoteModel(property: NoteProperty) {
        // Close old session
        getKoin().getScopeOrNull(Constants.SCOPE_ID_NOTE)?.close()
        // Create new session
        val noteSession = getKoin()
            .getOrCreateScope(Constants.SCOPE_ID_NOTE, named(Constants.SCOPE_NAME_NOTE))
        val noteModel: NoteModel = noteSession.get()
        noteModel.init(property.fileName, property.title)
        Log.d(TAG, "create NoteModel")
    }

    companion object {
        private const val TAG = "NoteListViewModel"
    }
}
