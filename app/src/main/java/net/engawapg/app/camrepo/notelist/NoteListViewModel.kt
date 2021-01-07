package net.engawapg.app.camrepo.notelist

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import net.engawapg.app.camrepo.model.NoteListModel
import net.engawapg.app.camrepo.model.NoteProperty
import net.engawapg.app.camrepo.util.Event
import java.text.DateFormat
import java.text.SimpleDateFormat

data class NoteListItem(
    val fileName: String,
    val title: String,
    val dateString: String,
    var select: Boolean = false
) {
    constructor(noteProperty: NoteProperty): this(
        noteProperty.fileName, noteProperty.title, dateFormat.format(noteProperty.updatedDate)
    )

    companion object {
        val dateFormat: DateFormat = SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.MEDIUM,
            SimpleDateFormat.MEDIUM)
    }
}

class NoteListViewModel(private val model: NoteListModel): ViewModel() {

    val onSelectNote = MutableLiveData<Event<String>>()
    val onCreateNote = MutableLiveData<Event<String>>()
    val editMode = MutableLiveData<Boolean>().apply { value = false }
    private var itemList: List<NoteListItem>

    init {
        itemList = model.list.map { noteProperty -> NoteListItem(noteProperty) }
    }

    fun createNewNote() {
        val note = model.createNewNote("", "")
        val item = NoteListItem(note)
        itemList = listOf(item) + itemList
        onCreateNote.value = Event(note.fileName)
    }

    fun selectNote(fileName: String) {
        if (editMode.value == false) {
            onSelectNote.value = Event(fileName)
        }
    }

    fun setEditMode(mode: Boolean) {
        if (!mode) {
            clearSelection()
        }
        editMode.value = mode
    }

    fun save() {
        model.save()
    }

    fun getItemCount() = model.list.size

    fun getItem(index: Int): NoteListItem {
        return itemList[index]
    }

    private fun clearSelection() {
        itemList.forEach { it.select = false }
    }

    fun isSelected(): Boolean {
        return itemList.any { it.select }
    }

    fun deleteSelectedItems() {
        val indexes = itemList.mapIndexedNotNull { index, item -> if (item.select) index else null }
        Log.d(TAG, "Delete at $indexes")
        model.deleteNotesAt(indexes)
        itemList = itemList.filter { item -> !item.select }
    }

    companion object {
        private const val TAG = "NoteListViewModel"
    }
}
