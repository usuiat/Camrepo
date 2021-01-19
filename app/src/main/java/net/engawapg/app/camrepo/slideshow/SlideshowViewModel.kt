package net.engawapg.app.camrepo.slideshow

import androidx.lifecycle.ViewModel
import net.engawapg.app.camrepo.model.NoteModel

class SlideshowViewModel(private val noteModel: NoteModel): ViewModel() {

    fun getSlideCount() = noteModel.getPageNum()
}