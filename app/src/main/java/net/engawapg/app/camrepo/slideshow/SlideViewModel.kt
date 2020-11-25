package net.engawapg.app.camrepo.slideshow

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import net.engawapg.app.camrepo.model.NoteModel

class SlideViewModel(app: Application, private val noteModel: NoteModel): AndroidViewModel(app) {

    var pageIndex: Int = 0

    fun getPageTitle() = noteModel.getTitle(pageIndex)

    fun getPageMemo() = noteModel.getMemo(pageIndex)

    fun getPhotoCount() = noteModel.getPhotoCount(pageIndex)

    fun getPhoto(photoIndex: Int) = noteModel.getPhotoAt(pageIndex, photoIndex)
}