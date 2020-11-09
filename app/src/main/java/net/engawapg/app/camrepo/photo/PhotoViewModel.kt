package net.engawapg.app.camrepo.photo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import net.engawapg.app.camrepo.model.ImageInfo
import net.engawapg.app.camrepo.model.NoteModel

class PhotoViewModel(app: Application, private val noteModel: NoteModel, val pageIndex: Int)
    : AndroidViewModel(app) {

    fun getPhotoCount() = noteModel.getPhotoCount(pageIndex)

    fun getPhotoAt(index: Int): ImageInfo? = noteModel.getPhotoAt(pageIndex, index)
}