package net.engawapg.app.camrepo.note

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import net.engawapg.app.camrepo.model.ImageInfo
import net.engawapg.app.camrepo.model.NoteListModel
import net.engawapg.app.camrepo.model.NoteModel
import net.engawapg.app.camrepo.util.Event

open class NoteItem(val viewType: Int)

class NoteTitleItem(var title: String, var subTitle: String)
    : NoteItem(NoteViewModel.VIEW_TYPE_TITLE)

class NotePageTitleItem(val pageTitle: String, val pageIndex: Int)
    : NoteItem(NoteViewModel.VIEW_TYPE_PAGE_TITLE) {
    var select = false
}

class NotePhotoItem(private val imageInfo: ImageInfo, val pageIndex: Int, val photoIndex: Int)
    : NoteItem(NoteViewModel.VIEW_TYPE_PHOTO) {
    val bmp = MutableLiveData<Bitmap>()
    fun loadPhoto(context: Context, defaultDrawableId: Int) {
        var bitmap = imageInfo.getBitmapThumbnailWithResolver(context.contentResolver)
        if (bitmap == null) {
            bitmap = BitmapFactory.decodeResource(context.resources, defaultDrawableId)
        }
        bmp.value = bitmap
    }
}

class NoteMemoItem(var memo: String, val pageIndex: Int): NoteItem(NoteViewModel.VIEW_TYPE_MEMO)

class NoteBlankItem(val pageIndex: Int): NoteItem(NoteViewModel.VIEW_TYPE_BLANK)

data class PhotoIndex(val pageIndex: Int, val photoIndex: Int)

class NoteViewModel(noteFileName: String, private val noteListModel: NoteListModel): ViewModel() {

    /* RecyclerViewを構成するアイテムのリスト */
    private val noteModel: NoteModel?
    val onClickTitle = MutableLiveData<Event<String>>()
    val onSelectPage = MutableLiveData<Event<Int>>()
    val onSelectPhoto = MutableLiveData<Event<PhotoIndex>>()
    val editMode = MutableLiveData<Boolean>().apply { value = false }
    private lateinit var itemList: List<NoteItem>
    private var columnCount: Int = 4

    init {
        Log.d(TAG, "NoteViewModel Init")
        val noteProperty = noteListModel.getNote(noteFileName)
        noteModel = NoteModel.createModel(noteProperty)
        buildItemList()
    }

    fun createNewPage() {
        Log.d(TAG, "createNewPage")
        val pageIndex = noteModel?.createNewPage()
        if (pageIndex != null) {
            buildItemList()
            onSelectPage.value = Event(pageIndex)
        }
    }

    fun selectItem(item: NoteItem) {
        if (editMode.value == false) {
            when (item) {
                is NoteTitleItem -> onClickTitle.value = Event("onClickTitle")
                is NotePageTitleItem -> onSelectPage.value = Event(item.pageIndex)
                is NotePhotoItem -> onSelectPhoto.value = Event(PhotoIndex(item.pageIndex, item.photoIndex))
                is NoteBlankItem -> onSelectPage.value = Event(item.pageIndex)
                is NoteMemoItem -> onSelectPage.value = Event(item.pageIndex)
            }
        }
    }

    fun setEditMode(mode: Boolean) {
        editMode.value = mode
        buildItemList()
    }

    fun buildItemList() {
        val list = mutableListOf<NoteItem>()
        if (noteModel != null) {
            if (editMode.value == false) {
                list.add(NoteTitleItem(noteModel.title, noteModel.subTitle)) // ノートタイトル
            }

            val n = noteModel.getPageNum()
            for (pageIdx in 0 until n) {
                list.add(NotePageTitleItem(noteModel.getTitle(pageIdx), pageIdx))
                if (editMode.value == true) {
                    continue
                }

                /* 写真 */
                val photoCount = noteModel.getPhotoCount(pageIdx)
                var count = 0
                for (photoIdx in 0 until photoCount) {
                    val info = noteModel.getPhotoAt(pageIdx, photoIdx)
                    if (info != null) {
                        list.add(NotePhotoItem(info, pageIdx, photoIdx))
                        count++
                    }
                }

                /* カードビューをいびつな形にしないための空欄 */
                val blankCount = columnCount - (count % columnCount)
                for (blankIdx in 0 until blankCount) {
                    list.add(NoteBlankItem(pageIdx))
                }

                list.add(NoteMemoItem(noteModel.getMemo(pageIdx), pageIdx))
            }
        }

        itemList = list
    }

    fun getItemCount() = itemList.size

    fun getViewType(index: Int): Int {
        return itemList[index].viewType
    }

    fun getItem(index: Int): NoteItem? = itemList[index]

    fun movePage(from:Int, to:Int) {
        noteModel?.movePage(from, to)
        buildItemList()
    }

    fun isPageSelected(): Boolean {
        return itemList.filterIsInstance<NotePageTitleItem>().any { it.select }
    }

    fun deleteSelectedPages() {
        val indexes = itemList.filterIsInstance<NotePageTitleItem>()
            .mapIndexedNotNull { index, item -> if (item.select) index else null }
        Log.d(TAG, "Delete at $indexes")
        noteModel?.deletePagesAt(indexes)
        buildItemList()
    }

//    fun isModifiedAfterLastDisplayedTime(): Boolean {
//        val date = noteModel?.let {
//            noteListModel.getNote(it.fileName)?.updatedDate ?: 0
//        } ?: 0
//        return (date != 0L) && (lastModifiedDate != 0L) && (date != lastModifiedDate)
//    }

    fun save() {
        noteModel?.let {
            it.save()
            noteListModel.updateLastModifiedDate(it.fileName)
            noteListModel.save()
//            lastModifiedDate = noteListModel.getNote(it.fileName)?.updatedDate ?: 0
        }
    }

    companion object {
        const val VIEW_TYPE_TITLE = 1
        const val VIEW_TYPE_PAGE_TITLE = 2
        const val VIEW_TYPE_PHOTO = 3
        const val VIEW_TYPE_MEMO = 4
        const val VIEW_TYPE_BLANK = 5

        private const val TAG = "NoteViewModel"
    }
}