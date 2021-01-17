package net.engawapg.app.camrepo.page

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import net.engawapg.app.camrepo.R
import net.engawapg.app.camrepo.model.ImageInfo
import net.engawapg.app.camrepo.model.NoteListModel
import net.engawapg.app.camrepo.model.NoteModel
import net.engawapg.app.camrepo.util.Event

open class PageItem(val viewType: Int)

class PagePhotoItem(private val imageInfo: ImageInfo, val photoIndex: Int)
    : PageItem(PageViewModel.VIEW_TYPE_PHOTO) {
    val bmp = MutableLiveData<Bitmap>()
    var select = false
    fun loadPhoto(context: Context, defaultDrawableId: Int) {
        var bitmap = imageInfo.getBitmapThumbnailWithResolver(context.contentResolver)
        if (bitmap == null) {
            bitmap = BitmapFactory.decodeResource(context.resources, defaultDrawableId)
        }
        bmp.value = bitmap
    }
}

class PageViewModel(private val noteModel: NoteModel,
                    private val noteListModel: NoteListModel, private val pageIndex: Int,
                    private val columnCount: Int)
    : ViewModel() {

    val onClickPhoto = MutableLiveData<Event<Int>>()
    val onClickAddPhoto = MutableLiveData<Event<String>>()
    val onEditTextFocused = MutableLiveData<Event<String>>()
    val editMode = MutableLiveData<Boolean>().apply { value = false }
    private lateinit var itemList: List<PageItem>

    var title: String
        get() = noteModel.getTitle(pageIndex)
        set(value) {
            noteModel.setTitle(pageIndex, value)
        }

    var memo: String
        get() = noteModel.getMemo(pageIndex)
        set(value) {
            noteModel.setMemo(pageIndex, value)
        }

    init {
        buildItemList()
    }

    fun reload() {
        buildItemList()
    }

    fun selectPhotoItem(item: PagePhotoItem) {
        if (editMode.value == false) {
            onClickPhoto.value = Event(item.photoIndex)
        }
    }

    fun onClick(view: View) {
        when (view.id) {
            R.id.addPhotoCardView -> onClickAddPhoto.value = Event("onClickAddPhoto")
        }
    }

    fun onEditTextFocusChange(@Suppress("UNUSED_PARAMETER") view: View, hasFocus: Boolean) {
        if (hasFocus) {
            onEditTextFocused.value = Event("onEditTextFocused")
        }
    }

    fun setEditMode(mode: Boolean) {
        editMode.value = mode
        buildItemList()
    }

    private fun buildItemList() {
        val list = mutableListOf<PageItem>()

        /* Title */
        if (editMode.value == false) {
            list.add(PageItem(VIEW_TYPE_PAGE_TITLE))
        }

        /* Photo */
        val n = noteModel.getPhotoCount(pageIndex)
        var count = 0
        for (index in 0 until n) {
            noteModel.getPhotoAt(pageIndex, index)?.let { info ->
                list.add(PagePhotoItem(info, index))
                count++
            }
        }
        /* Add Photo */
        if (editMode.value == false) {
            list.add(PageItem(VIEW_TYPE_ADD_PHOTO))
            count++
        }

        /* カードビューをいびつな形にしないための空間 */
        val blankCount = columnCount - (count % columnCount)
        for (i in 0 until blankCount) {
            list.add(PageItem(VIEW_TYPE_BLANK))
        }

        /* Memo */
        if (editMode.value == false) {
            list.add(PageItem(VIEW_TYPE_MEMO))
        }

        itemList = list
    }

    fun getItemCount(): Int {
        return itemList.size
    }

    fun getViewType(position: Int) :Int {
        return itemList[position].viewType
    }

    fun getPhotoItem(position: Int): PagePhotoItem? {
        val item = itemList[position]
        return if (item is PagePhotoItem) item else null
    }

    fun movePhoto(from: Int, to: Int) {
        noteModel.movePhoto(pageIndex, from, to)
        buildItemList()
    }

    fun isPhotoSelected() = itemList.filterIsInstance<PagePhotoItem>().any{ it.select }

    fun deleteSelectedPhotos() {
        val indexes = itemList.filterIsInstance<PagePhotoItem>()
            .mapIndexedNotNull { index, item -> if (item.select) index else null }
        Log.d(TAG, "Delete at $indexes")
        noteModel.deletePhotosAt(pageIndex, indexes)
        buildItemList()
    }

    fun save(){
        noteModel.save()
        noteListModel.updateLastModifiedDate(noteModel.fileName)
        noteListModel.save()
    }

    companion object {
        private const val TAG = "PageViewModel"

        const val VIEW_TYPE_PAGE_TITLE = 1
        const val VIEW_TYPE_PHOTO = 2
        const val VIEW_TYPE_MEMO = 3
        const val VIEW_TYPE_ADD_PHOTO = 4
        const val VIEW_TYPE_BLANK = 5
    }
}