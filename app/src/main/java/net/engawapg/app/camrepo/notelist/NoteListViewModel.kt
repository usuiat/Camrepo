package net.engawapg.app.camrepo.notelist

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import net.engawapg.app.camrepo.model.NoteListModel
import net.engawapg.app.camrepo.model.NoteModel
import net.engawapg.app.camrepo.model.NoteProperty
import java.text.SimpleDateFormat

class NoteListViewModel(app: Application, private val model: NoteListModel)
    : AndroidViewModel(app) {

    private var selection: MutableList<Boolean>? = null
    private var lastModified: Long = 0
    private var currentNote: NoteProperty? = null

    fun createNewNote(title: String, subTitle: String) {
        val note = model.createNewNote(title, subTitle)
        lastModified = note.updatedDate
        currentNote = note
        Log.d(TAG, "updateDate = $lastModified")
        NoteModel.createModel(note)
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
        lastModified = note.updatedDate
        currentNote = note
        Log.d(TAG, "updateDate = $lastModified")
        NoteModel.createModel(note)
    }

    fun initSelection() {
        selection = MutableList(getItemCount()){false}
    }

    fun clearSelection() {
        selection = null
    }

    fun setSelection(index: Int, sel: Boolean) {
        selection?.let {
            if (index < it.size) {
                it[index] = sel
                Log.d(TAG, "setSelection at $index, $sel")
            }
        }
    }

    fun getSelection(index: Int): Boolean {
        return selection?.getOrNull(index) ?: false
    }

    fun isSelected(): Boolean {
        return selection?.contains(true) ?: false
    }

    fun deleteSelectedItems() {
        val indexes = mutableListOf<Int>()
        selection?.forEachIndexed { index, b -> if (b) indexes.add(index) }
        Log.d(TAG, "Delete at $indexes")
        model.deleteNotesAt(indexes)
        clearSelection()
    }

    fun isCurrentNoteModified(): Boolean {
        Log.d(TAG, "updateDate = ${currentNote?.updatedDate}")
        val date = currentNote?.updatedDate
        return (date != null) && (date != lastModified)
    }

    companion object {
        private const val TAG = "NoteListViewModel"
    }
}
