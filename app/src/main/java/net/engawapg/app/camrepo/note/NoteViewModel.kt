package net.engawapg.app.camrepo.note

import android.content.ContentResolver
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import net.engawapg.app.camrepo.model.ImageInfo
import net.engawapg.app.camrepo.model.NoteListModel
import net.engawapg.app.camrepo.model.NoteModel

class NoteViewModel(private val noteModel: NoteModel, private val noteListModel: NoteListModel)
    : ViewModel() {

    data class ItemInfo(
        val viewType: Int,  /* RecyclerViewのViewType */
        val pageIndex: Int, /* ページ番号 */
        val subIndex: Int,   /* ページ内の要素（写真）の番号 */
        var selected: Boolean
    )

    /* RecyclerViewを構成するアイテムのリスト */
    private lateinit var itemList: MutableList<ItemInfo>
    private var columnCount: Int = 4
    private var modified = false
    private var pageAdded = false
    private var lastModifiedDate: Long = 0
    private var pageTitleListMode = false /* ページタイトルの一覧を表示するモード */

    fun initItemList(columnCount: Int) {
        this.columnCount = columnCount
        buildItemList()
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

        val n = noteModel.getPageNum()
        for (pageIdx in 0 until n) {
            list.add(ItemInfo(VIEW_TYPE_PAGE_TITLE, pageIdx, 0, false)) /* ページの先頭はページタイトル */

            if (pageTitleListMode) {
                continue
            }

            /* 写真 */
            val photoCount = noteModel.getPhotoCount(pageIdx)
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
        Log.d(TAG, "before createNewPage: itemList.size = ${itemList.size}")
        noteModel.createNewPage()
        buildItemList()
        modified = true
        pageAdded = true
        Log.d(TAG, "after  createNewPage: itemList.size = ${itemList.size}")
    }

    fun isPageAdded(): Boolean {
        val added = pageAdded
        pageAdded = false
        return added
    }

    fun movePage(from:Int, to:Int) {
        noteModel.movePage(from, to)
        modified = true
    }

    fun setPageSelection(index: Int, sel: Boolean) {
        if (index < itemList.size) {
            itemList[index].selected = sel
        }
    }

    fun getPageSelection(index: Int) = itemList.getOrNull(index)?.selected ?: false

    fun isPageSelected(): Boolean {
        return itemList.find { item -> item.selected } != null
    }

    fun deleteSelectedPages() {
        val indexes = mutableListOf<Int>()
        itemList.forEachIndexed { index, item -> if (item.selected) indexes.add(index) }
        Log.d(TAG, "Delete at $indexes")
        noteModel.deletePagesAt(indexes)
        modified = true
    }

    fun getNoteTitle() = noteModel.title
    fun getNoteSubTitle() = noteModel.subTitle

    fun setNoteTitle(title: String, subTitle: String) {
        noteModel.title = title
        noteModel.subTitle = subTitle
        noteListModel.updateNoteTitle(noteModel.fileName, title, subTitle)
        modified = true
    }

    fun getItemCount() = itemList.size

    fun getViewType(index: Int): Int {
        return itemList[index].viewType
    }

    fun getPageIndex(itemIndex: Int) = itemList[itemIndex].pageIndex

    fun getPageTitle(itemIndex: Int): String {
        val pageIndex = itemList[itemIndex].pageIndex
        return noteModel.getTitle(pageIndex)
    }

    fun getMemo(itemIndex: Int): String {
        val pageIndex = itemList[itemIndex].pageIndex
        return noteModel.getMemo(pageIndex)
    }

    fun getPhotoIndex(itemIndex: Int) = itemList[itemIndex].subIndex

    private fun getPhoto(itemIndex: Int): ImageInfo? {
        val pageIndex = itemList[itemIndex].pageIndex
        val photoIndex = itemList[itemIndex].subIndex
        return noteModel.getPhotoAt(pageIndex, photoIndex)
    }

    fun getPhotoBitmap(itemIndex: Int, resolver: ContentResolver): Bitmap? {
        val imageInfo = getPhoto(itemIndex)
        return imageInfo?.getBitmapThumbnailWithResolver(resolver)
    }

    fun isModifiedAfterLastDisplayedTime(): Boolean {
        val date = noteListModel.getNote(noteModel.fileName)?.updatedDate ?: 0
        return (date != 0L) && (lastModifiedDate != 0L) && (date != lastModifiedDate)
    }

    fun save() {
        if (modified) {
            noteModel.save()
            modified = false
            noteListModel.updateLastModifiedDate(noteModel.fileName)
            noteListModel.save()
        }
        lastModifiedDate = noteListModel.getNote(noteModel.fileName)?.updatedDate ?: 0
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