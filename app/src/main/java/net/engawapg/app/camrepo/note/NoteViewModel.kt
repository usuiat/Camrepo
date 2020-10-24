package net.engawapg.app.camrepo.note

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import net.engawapg.app.camrepo.model.NoteListModel
import net.engawapg.app.camrepo.model.NoteModel

class NoteViewModel(app: Application, private val noteModel: NoteModel,
                    private val noteListModel: NoteListModel)
    : AndroidViewModel(app) {

    data class ItemInfo(
        val viewType: Int,  /* RecyclerViewのViewType */
        val pageIndex: Int, /* ページ番号 */
        val subIndex: Int   /* ページ内の要素（写真）の番号 */
    )

    /* RecyclerViewを構成するアイテムのリスト */
    private lateinit var itemList: MutableList<ItemInfo>
    private var columnCount: Int = 4
    private var modified = false

    fun initItemList(columnCount: Int) {
        this.columnCount = columnCount
        buildItemList()
    }

    private fun buildItemList() {
        val list = mutableListOf<ItemInfo>()
        list.add(ItemInfo(VIEW_TYPE_TITLE, 0, 0)) /* 先頭はタイトル */

        val n = noteModel.getPageNum()
        for (pageIdx in 0 until n) {
            list.add(ItemInfo(VIEW_TYPE_PAGE_TITLE, pageIdx, 0)) /* ページの先頭はページタイトル */

            /* 写真 */
            val photoCount = noteModel.getPhotoCount(pageIdx)
            for (photoIdx in 0 until photoCount) {
                list.add(ItemInfo(VIEW_TYPE_PHOTO, pageIdx, photoIdx))
            }

            /* カードビューをいびつな形にしないための空欄 */
            val blankCount = columnCount - (photoCount % columnCount)
            for (blankIdx in 0 until blankCount) {
                list.add(ItemInfo(VIEW_TYPE_BLANK, pageIdx, blankIdx))
            }

            list.add(ItemInfo(VIEW_TYPE_MEMO, pageIdx, 0)) /* メモ欄 */
        }

        itemList = list
    }

    fun addPage() {
        noteModel.createNewPage()
        buildItemList()
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
    fun setPageTitle(itemIndex: Int, t: String) {
        val pageIndex = itemList[itemIndex].pageIndex
        noteModel.setTitle(pageIndex, t)
        modified = true
    }

    fun getMemo(itemIndex: Int): String {
        val pageIndex = itemList[itemIndex].pageIndex
        return noteModel.getMemo(pageIndex)
    }
    fun setMemo(itemIndex: Int, m:String) {
        val pageIndex = itemList[itemIndex].pageIndex
        noteModel.setMemo(pageIndex, m)
        modified = true
    }

    fun save() {
        noteModel.save()
        if (modified) {
            modified = false
            noteListModel.updateLastModifiedDate(noteModel.fileName)
            noteListModel.save()
        }
    }

    companion object {
        const val VIEW_TYPE_TITLE = 1
        const val VIEW_TYPE_PAGE_TITLE = 2
        const val VIEW_TYPE_PHOTO = 3
        const val VIEW_TYPE_MEMO = 4
        const val VIEW_TYPE_BLANK = 5
    }
}