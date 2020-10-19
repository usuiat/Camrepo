package net.engawapg.app.camrepo.model

import java.util.*

class NoteProperty(var title: String,
                   var subTitle: String,
                   var fileName: String,
                   val creationDate: Long,
                   var updatedDate: Long) {

    companion object {
        fun createNewNote(title: String) :NoteProperty {
            val date = Date(System.currentTimeMillis()).time
            return NoteProperty(title, "", date.toString(), date, date)
        }
    }

    fun updateLastModifiedDate() {
        val date = Date(System.currentTimeMillis()).time
        updatedDate = date
    }
}