package net.engawapg.app.camrepo.slideshow

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import net.engawapg.app.camrepo.model.NoteModel

class SlideshowViewModel(app: Application, private val noteModel: NoteModel)
    : AndroidViewModel(app) {

    fun getSlideCount() = noteModel.getPageNum()
}