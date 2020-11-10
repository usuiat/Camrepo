package net.engawapg.app.camrepo.photo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import net.engawapg.app.camrepo.model.ImageInfo
import net.engawapg.app.camrepo.model.NoteModel

class PhotoViewModel(app: Application, private val noteModel: NoteModel)
    : AndroidViewModel(app) {

    data class PhotoInfo(val pageIndex: Int, val photoIndex: Int)

    private var pageIndex = 0
    private var wholeOfNote = false
    private val photoList = mutableListOf<PhotoInfo>()

    fun initModel(pageIdx: Int) {
        pageIndex = pageIdx
        if (pageIdx < 0){
            wholeOfNote = true
        }

        if (wholeOfNote) {
            val pageNum = noteModel.getPageNum()
            for (page in 0 until pageNum) {
                val photoNum = noteModel.getPhotoCount(page)
                for (photo in 0 until photoNum) {
                    photoList.add(PhotoInfo(page, photo))
                }
            }
        }
    }

    fun getPosition(pageIdx: Int, photoIdx: Int): Int {
        return if (wholeOfNote) {
            photoList.indexOfFirst {
                (it.pageIndex == pageIdx) and (it.photoIndex == photoIdx)
            }
        } else {
            photoIdx
        }
    }

    fun getPhotoCount(): Int {
        return if (wholeOfNote) {
            photoList.size
        } else {
            noteModel.getPhotoCount(pageIndex)
        }
    }

    fun getPhotoAt(index: Int): ImageInfo? {
        return if (wholeOfNote) {
            val info = photoList[index]
            noteModel.getPhotoAt(info.pageIndex, info.photoIndex)
        } else {
            noteModel.getPhotoAt(pageIndex, index)
        }
    }
}