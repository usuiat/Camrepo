package net.engawapg.app.camrepo.slideshow

import androidx.lifecycle.ViewModel
import net.engawapg.app.camrepo.model.NoteModel

class SlideViewModel(private val noteModel: NoteModel): ViewModel() {

    var pageIndex: Int = 0

    fun getPageTitle() = noteModel.getTitle(pageIndex)

    fun getPageMemo() = noteModel.getMemo(pageIndex)

    fun getPhotoCount(): Int {
        val memo = getPageMemo()
        val maxCount = if (memo.isEmpty()) 50 else 25
        val count = noteModel.getPhotoCount(pageIndex)
        return if (count < maxCount) count else maxCount
    }

    fun getPhoto(photoIndex: Int) = noteModel.getPhotoAt(pageIndex, photoIndex)
}