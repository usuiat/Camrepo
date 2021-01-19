package net.engawapg.app.camrepo.photo

import androidx.lifecycle.ViewModel
import net.engawapg.app.camrepo.model.ImageInfo
import net.engawapg.app.camrepo.model.NoteModel

class PhotoViewModel(private val noteModel: NoteModel): ViewModel() {

    data class PhotoInfo(val pageIndex: Int, val photoIndex: Int)

    private var pageIndex = 0
    private var wholeOfNote = false
    private lateinit var photoList: MutableList<PhotoInfo>

    fun initModel(pageIdx: Int) {
        pageIndex = pageIdx
        wholeOfNote = (pageIdx < 0)

        if (wholeOfNote) {
            photoList = mutableListOf()
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

    fun getTitle(): String {
        return if (wholeOfNote) {
            noteModel.title
        } else {
            noteModel.getTitle(pageIndex)
        }
    }
}