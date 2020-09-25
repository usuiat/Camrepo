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
import java.text.SimpleDateFormat

class NoteListViewModel(app: Application, private val model: NoteListModel)
    : AndroidViewModel(app) {

    fun onClickAdd() {
        Log.d(TAG, "add")
        createNoteModel(model.createNewNote("New Note ${model.list.size + 1}"))
    }

    fun save() {
        model.save()
    }

    fun getItemCount() = model.list.size

    private fun getItem(index: Int) = model.list[index]

    fun getTitle(index: Int) = getItem(index).title

    fun getSubTitle(index: Int) = getItem(index).subTitle

    fun getUpdateDate(index: Int) :String {
        val date = getItem(index).updatedDate
        val df = SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.MEDIUM,
                                                      SimpleDateFormat.MEDIUM)
        return df.format(date)
    }

    fun selectNote(index: Int) {
        val note = getItem(index)
        createNoteModel(note)
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
