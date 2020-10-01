package net.engawapg.app.camrepo.model

import android.app.Application
import android.util.JsonReader
import android.util.JsonWriter
import android.util.Log
import java.io.*
import java.lang.Exception
import java.util.*

class NoteListModel(private val app: Application) {
    val list = mutableListOf<NoteProperty>()

    init {
        load()
    }

    private fun load() {
        list.clear()
        val file = File(app.filesDir, NOTE_LIST_FILE_NAME)
        try {
            JsonReader(BufferedReader(FileReader(file))).use { reader ->
                reader.beginArray()
                while (reader.hasNext()) {
                    list.add(loadNoteProperty(reader))
                }
                reader.endArray()
            }
        } catch (e: Exception) {
            Log.e(TAG, e.toString())
        }
    }

    private fun loadNoteProperty(reader: JsonReader) : NoteProperty {
        var title = ""
        var creationDate = 0L
        var updatedDate = 0L
        var fileName = ""

        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "title" -> title = reader.nextString()
                "creationDate" -> creationDate = reader.nextLong()
                "updatedDate" -> updatedDate = reader.nextLong()
                "fileName" -> fileName = reader.nextString()
                else -> reader.skipValue()
            }
        }
        reader.endObject()
        return NoteProperty(title, "", fileName, creationDate, updatedDate)
    }

    fun save() {
        val file = File(app.filesDir, NOTE_LIST_FILE_NAME)
        JsonWriter(BufferedWriter(FileWriter(file))).use { writer ->
            writer.setIndent("    ")
            writer.beginArray()
            list.forEach {
                saveNoteProperty(writer, it)
            }
            writer.endArray()
        }
    }

    private fun saveNoteProperty(writer: JsonWriter, note: NoteProperty) {
        writer.beginObject()
        writer.name("title").value(note.title)
        writer.name("creationDate").value(note.creationDate)
        writer.name("updatedDate").value(note.updatedDate)
        writer.name("fileName").value(note.fileName)
        writer.endObject()
    }

    fun createNewNote(title: String): NoteProperty {
        val note = NoteProperty.createNewNote(title)
        list.add(note)
        return note
    }

    fun deleteNotesAt(indexes: List<Int>) {
        val sorted = indexes.sortedByDescending { it }
        sorted.forEach{ i ->
            list.removeAt(i)
        }
    }

    companion object {
        private const val NOTE_LIST_FILE_NAME = "noteList.json"
        private const val TAG = "NoteListModel"
    }
}