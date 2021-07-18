package net.engawapg.app.camrepo.page

import android.content.ContentResolver
import android.graphics.Bitmap
import android.util.Log
import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import net.engawapg.app.camrepo.model.ImageInfo
import net.engawapg.app.camrepo.model.NoteListModel
import net.engawapg.app.camrepo.model.NoteModel
import net.engawapg.app.camrepo.util.Event

open class PageItem(val viewType: Int)

class PagePhotoItem(val imageInfo: ImageInfo, val photoIndex: Int)
    : PageItem(PageViewModel.VIEW_TYPE_PHOTO) {
    var select = false
}

class PageViewModel(private val noteModel: NoteModel, private val noteListModel: NoteListModel,
                    val pageIndex: Int, private val columnCount: Int): ViewModel() {

    var modified = false
    val uiEvent = MutableLiveData<Event<Int>>()
    val photoClickEvent = MutableLiveData<Event<Int>>()
    val editMode = MutableLiveData<Boolean>()
    private var itemList: List<PageItem>

    init {
        editMode.value = false
        itemList = buildItemList()
    }

    fun reload() {
        itemList = buildItemList()
    }

    fun setEditMode(mode: Boolean) {
        editMode.value = mode
        itemList = buildItemList()
    }

    private fun buildItemList(): List<PageItem> {
        val list = mutableListOf<PageItem>()

        /* Title */
        if (editMode.value == false) {
            list.add(PageItem(VIEW_TYPE_PAGE_TITLE))
        }

        /* Photo */
        var n = noteModel.getPhotoCount(pageIndex)
        for (index in 0 until n) {
            noteModel.getPhotoAt(pageIndex, index)?.let { imageInfo ->
                list.add(PagePhotoItem(imageInfo, index))
            }
        }

        /* Add Photo */
        if (editMode.value == false) {
            list.add(PageItem(VIEW_TYPE_ADD_PHOTO))
            n++
        }

        /* Blank */
        val nBlank = columnCount - (n % columnCount)
        for (i in 0 until nBlank) {
            list.add(PageItem(VIEW_TYPE_BLANK))
        }

        /* Memo */
        if (editMode.value == false) {
            list.add(PageItem(VIEW_TYPE_MEMO))
        }

        return list
    }

    fun getItemCount(): Int {
        return itemList.size
    }

    fun getViewType(position: Int) :Int {
        return itemList[position].viewType
    }

    var pageTitle: String
        get() = noteModel.getTitle(pageIndex)
        set(value) {
            val oldTitle = noteModel.getTitle(pageIndex)
            if (value != oldTitle) {
                noteModel.setTitle(pageIndex, value)
                modified = true
            }
        }

    var memo: String
        get() = noteModel.getMemo(pageIndex)
        set(value) {
            val oldMemo = noteModel.getMemo(pageIndex)
            if (value != oldMemo) {
                noteModel.setMemo(pageIndex, value)
                modified = true
            }
        }

    fun getPhotoItem(index: Int): PagePhotoItem? {
        val item = itemList[index]
        return if (item is PagePhotoItem) {
            item
        } else {
            null
        }
    }

    fun getPhotoBitmap(index: Int, resolver: ContentResolver): Bitmap? {
        val item = itemList[index]
        return if (item is PagePhotoItem) {
            item.imageInfo.getBitmapThumbnailWithResolver(resolver)
        } else {
            null
        }
    }

    fun movePhoto(from: Int, to: Int) {
        noteModel.movePhoto(pageIndex, from, to)
        modified = true
    }

    fun isPhotoSelected(): Boolean {
        return itemList.any {(it is PagePhotoItem) && it.select }
    }

    fun deleteSelectedPhotos() {
        val indexes = itemList.mapNotNull {
            if ((it is PagePhotoItem) && it.select) it.photoIndex else null
        }
        Log.d(TAG, "Delete at $indexes")
        noteModel.deletePhotosAt(pageIndex, indexes)
        modified = true
    }

    fun addImageInfo(info: ImageInfo) {
        noteModel.addPhotoAt(pageIndex, info)
        reload()
        modified = true
    }

    fun save(){
        if (modified) {
            Log.d(TAG, "Note modified")
            modified = false
            noteModel.save()
            noteListModel.updateLastModifiedDate(noteModel.fileName)
            noteListModel.save()
        }
    }

    fun onClickAddPicture() {
        uiEvent.value = Event(UI_EVENT_ON_CLICK_ADD_PICTURE)
    }

    fun onClickTakePicture() {
        uiEvent.value = Event(UI_EVENT_ON_CLICK_TAKE_PICTURE)
    }

    fun onClickPicture(item: PagePhotoItem) {
        if (editMode.value == false) {
            photoClickEvent.value = Event(item.photoIndex)
        }
    }

    fun onFocusChangeToTextEdit(@Suppress("UNUSED_PARAMETER")view: View, hasFocus: Boolean) {
        if (hasFocus) {
            uiEvent.value = Event(UI_EVENT_ON_FOCUS_CHANGE_TO_TEXT_EDIT)
        }
    }

    companion object {
        private const val TAG = "PageViewModel"

        const val VIEW_TYPE_PAGE_TITLE = 1
        const val VIEW_TYPE_PHOTO = 2
        const val VIEW_TYPE_MEMO = 3
        const val VIEW_TYPE_ADD_PHOTO = 4
        const val VIEW_TYPE_BLANK = 5

        const val UI_EVENT_ON_CLICK_ADD_PICTURE = 1
        const val UI_EVENT_ON_CLICK_TAKE_PICTURE = 2
        const val UI_EVENT_ON_FOCUS_CHANGE_TO_TEXT_EDIT = 3
    }
}