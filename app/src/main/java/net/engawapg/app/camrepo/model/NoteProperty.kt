package net.engawapg.app.camrepo.model

data class NoteProperty(var title: String,
                        var fileName: String,
                        val creationDate: Long,
                        var updatedDate: Long)