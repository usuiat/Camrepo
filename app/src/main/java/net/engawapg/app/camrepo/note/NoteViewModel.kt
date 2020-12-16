package net.engawapg.app.camrepo.note

import android.app.Application
import android.content.ContentResolver
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import net.engawapg.app.camrepo.model.ImageInfo
import net.engawapg.app.camrepo.model.NoteListModel
import net.engawapg.app.camrepo.model.NoteModel
import net.engawapg.app.camrepo.model.NoteProperty

class NoteViewModel(app: Application, private val noteListModel: NoteListModel)
    : AndroidViewModel(app) {

    data class ItemInfo(
        val viewType: Int,  /* RecyclerViewのViewType */
        val pageIndex: Int, /* ページ番号 */
        val subIndex: Int,   /* ページ内の要素（写真）の番号 */
        var selected: Boolean
    )

    /* RecyclerViewを構成するアイテムのリスト */
    val noteProperty = MutableLiveData<NoteProperty?>()
    private var noteModel: NoteModel? = null
    private var itemList: MutableList<ItemInfo>? = null
    var columnCount: Int = 4
    private var modified = false
    private var pageAdded = false
    private var lastModifiedDate: Long = 0
    private var pageTitleListMode = false /* ページタイトルの一覧を表示するモード */

    fun initItemList() {
        noteProperty.value?.let {
            noteModel = NoteModel.createModel(it)
            buildItemList()
        }
    }

    fun setPageTitleListMode(mode: Boolean) {
        pageTitleListMode = mode
        buildItemList()
    }

    fun buildItemList() {
        val list = mutableListOf<ItemInfo>()
        if (!pageTitleListMode) {
            list.add(ItemInfo(VIEW_TYPE_TITLE, 0, 0, false)) /* 先頭はタイトル */
        }

        val n = noteModel?.getPageNum() ?: 0
        for (pageIdx in 0 until n) {
            list.add(ItemInfo(VIEW_TYPE_PAGE_TITLE, pageIdx, 0, false)) /* ページの先頭はページタイトル */

            if (pageTitleListMode) {
                continue
            }

            /* 写真 */
            val photoCount = noteModel?.getPhotoCount(pageIdx) ?: 0
            for (photoIdx in 0 until photoCount) {
                list.add(ItemInfo(VIEW_TYPE_PHOTO, pageIdx, photoIdx, false))
            }

            /* カードビューをいびつな形にしないための空欄 */
            val blankCount = columnCount - (photoCount % columnCount)
            for (blankIdx in 0 until blankCount) {
                list.add(ItemInfo(VIEW_TYPE_BLANK, pageIdx, blankIdx, false))
            }

            list.add(ItemInfo(VIEW_TYPE_MEMO, pageIdx, 0, false)) /* メモ欄 */
        }

        itemList = list
    }

    fun addPage() {
        Log.d(TAG, "before createNewPage: itemList.size = ${itemList?.size}")
        noteModel?.createNewPage()
        buildItemList()
        modified = true
        pageAdded = true
        Log.d(TAG, "after  createNewPage: itemList.size = ${itemList?.size}")
    }

    fun isPageAdded(): Boolean {
        val added = pageAdded
        pageAdded = false
        return added
    }

    fun movePage(from:Int, to:Int) {
        noteModel?.movePage(from, to)
        modified = true
    }

    fun setPageSelection(index: Int, sel: Boolean) {
        itemList?.let {
            if (index < it.size) {
                it[index].selected = sel
            }
        }
    }

    fun getPageSelection(index: Int) = itemList?.getOrNull(index)?.selected ?: false

    fun isPageSelected(): Boolean {
        return itemList?.find { item -> item.selected } != null
    }

    fun deleteSelectedPages() {
        val indexes = mutableListOf<Int>()
        itemList?.forEachIndexed { index, item -> if (item.selected) indexes.add(index) }
        Log.d(TAG, "Delete at $indexes")
        noteModel?.deletePagesAt(indexes)
        modified = true
    }

    fun getNoteTitle() = noteModel?.title ?: ""
    fun getNoteSubTitle() = noteModel?.subTitle ?: ""

    fun setNoteTitle(title: String, subTitle: String) {
        noteModel?.title = title
        noteModel?.subTitle = subTitle
        noteModel?.let {
            noteListModel.updateNoteTitle(it.fileName, title, subTitle)
        }
        modified = true
    }

    fun getItemCount() = itemList?.size ?: 0

    fun getViewType(index: Int): Int {
        return itemList?.let { it[index].viewType } ?: 0
    }

    fun getPageIndex(itemIndex: Int) = itemList?.let { it[itemIndex].pageIndex } ?: 0

    fun getPageTitle(itemIndex: Int): String {
        return itemList?.let {
            val pageIndex = it[itemIndex].pageIndex
            noteModel?.getTitle(pageIndex) ?: ""
        } ?: ""
    }

    fun getMemo(itemIndex: Int): String {
        return itemList?.let {
            val pageIndex = it[itemIndex].pageIndex
            noteModel?.getMemo(pageIndex) ?: ""
        } ?: ""
    }

    fun getPhotoIndex(itemIndex: Int) = itemList?.let { it[itemIndex].subIndex } ?: 0

    private fun getPhoto(itemIndex: Int): ImageInfo? {
        return itemList?.let {
            val pageIndex = it[itemIndex].pageIndex
            val photoIndex = it[itemIndex].subIndex
            noteModel?.getPhotoAt(pageIndex, photoIndex)
        }
    }

    fun getPhotoBitmap(itemIndex: Int, resolver: ContentResolver): Bitmap? {
        val imageInfo = getPhoto(itemIndex)
        return imageInfo?.getBitmapThumbnailWithResolver(resolver)
    }

    fun isModifiedAfterLastDisplayedTime(): Boolean {
        val date = noteModel?.let {
            noteListModel.getNote(it.fileName)?.updatedDate ?: 0
        } ?: 0
        return (date != 0L) && (lastModifiedDate != 0L) && (date != lastModifiedDate)
    }

    fun save() {
        noteModel?.let {
            if (modified) {
                it.save()
                modified = false
                noteListModel.updateLastModifiedDate(it.fileName)
                noteListModel.save()
            }
            lastModifiedDate = noteListModel.getNote(it.fileName)?.updatedDate ?: 0
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