package net.engawapg.app.camrepo.model

import android.app.Application
import android.net.Uri
import android.util.JsonReader
import android.util.JsonWriter
import android.util.Log
import net.engawapg.app.camrepo.Constants
import net.engawapg.app.camrepo.R
import org.koin.core.qualifier.named
import org.koin.java.KoinJavaComponent.getKoin
import java.io.*

class NoteModel(private val app: Application) {

    lateinit var fileName: String
    lateinit var title: String
    lateinit var subTitle: String
    private var pageSerialNumber: Int  = 0
    fun init(_fileName: String, _title: String, _subTitle:String) {
        fileName = _fileName
        title = _title
        subTitle = _subTitle
        if(!load()) {
            /* ファイルが存在しない（新規作成）の場合は空のページを一つ作る */
            createNewPage()
        }
    }

    private val pages = mutableListOf<PageInfo>()
    fun getPageNum() = pages.size
    fun getPage(index: Int) = pages.getOrNull(index)

    fun getMemo(pageIndex: Int): String = getPage(pageIndex)?.memo ?: ""
    fun setMemo(pageIndex: Int, value: String) {
        getPage(pageIndex)?.memo = value
    }

    fun getTitle(pageIndex: Int): String = getPage(pageIndex)?.title ?: ""
    fun setTitle(pageIndex: Int, value: String) {
        getPage(pageIndex)?.title = value
    }

    fun getPhotoCount(pageIndex: Int): Int = getPage(pageIndex)?.photos?.size ?: 0

    fun getPhotoAt(pageIndex: Int, photoIndex: Int) = getPage(pageIndex)?.getPhotoAt(photoIndex)

    fun addPhotoAt(pageIndex: Int, imageInfo: ImageInfo) {
        getPage(pageIndex)?.addPhoto(imageInfo)
        Log.d(TAG, "addPhoto page=$pageIndex")
    }

    fun deletePhotosAt(pageIndex: Int, indexes: List<Int>) {
        getPage(pageIndex)?.deletePhotosAt(indexes)
    }
    
    fun movePhoto(pageIndex: Int, from: Int, to: Int) {
        getPage(pageIndex)?.movePhoto(from, to)
    }

    /* 新しく作ったページのインデックスを返す */
    fun createNewPage(): Int {
        val page = PageInfo()
        pageSerialNumber++
        page.title = app.getString(R.string.default_page_title) + " $pageSerialNumber"
        pages.add(page)
        return pages.size - 1
    }

    fun deletePagesAt(indexes: List<Int>) {
        val sorted = indexes.sortedByDescending { it }
        for (i in sorted) {
            pages.removeAt(i)
        }
    }

    fun movePage(from: Int, to: Int) {
        val page = pages.removeAt(from)
        pages.add(to, page)
    }

    /* ファイルが存在しない場合は false を返す */
    private fun load(): Boolean {
        val file = File(app.filesDir, fileName)
        if (!file.exists()) {
            return false
        }

        try {
            JsonReader(BufferedReader(FileReader(file))).use { reader ->
                reader.beginObject()
                while (reader.hasNext()) {
                    when (reader.nextName()) {
                        "page_serial_number" -> {pageSerialNumber = reader.nextInt()}
                        "pages" -> loadPages(reader)
                        else -> reader.skipValue()
                    }
                }
                reader.endObject()
            }
        } catch (e: Exception) {
            Log.e(TAG, e.toString())
        }

        return true
    }

    private fun loadPages(reader: JsonReader) {
        pages.clear()
        try {
            reader.beginArray()
            while (reader.hasNext()) {
                reader.beginObject()
                val page = PageInfo()
                while (reader.hasNext()) {
                    when (reader.nextName()) {
                        "page_title" -> {page.title = reader.nextString()}
                        "memo" -> {page.memo = reader.nextString()}
                        "photos" -> {page.photos = loadPhotos(reader)}
                        else -> reader.skipValue()
                    }
                }
                pages.add(page)
                reader.endObject()
            }
            reader.endArray()
        }
        catch (e: Exception) {
            Log.e(TAG, e.toString())
        }
    }

    private fun loadPhotos(reader: JsonReader) : List<ImageInfo> {
        val list = mutableListOf<ImageInfo>()
        try {
            reader.beginArray()
            while (reader.hasNext()) {
                reader.beginObject()
                reader.nextName()
                val uri = Uri.parse(reader.nextString())
                list.add(ImageInfo(uri))
                reader.endObject()
            }
            reader.endArray()
        } catch (e: Exception) {
            Log.e(TAG, e.toString())
        }
        return list
    }

    fun save() {
        val file = File(app.filesDir, fileName)

        JsonWriter(BufferedWriter(FileWriter(file))).use { writer ->
            writer.setIndent("    ")
            writer.beginObject()
            writer.name("note_title").value(title)
            writer.name("page_serial_number").value(pageSerialNumber)
            writer.name("pages")
            savePages(writer)
            writer.endObject()
        }
        Log.d(TAG, "Save data!!")
    }

    private fun savePages(writer: JsonWriter) {
        writer.beginArray()
        pages.forEach { page ->
            writer.beginObject()
            writer.name("page_title").value(page.title)
            writer.name("memo").value(page.memo)
            writer.name("photos")
            writer.beginArray()
            page.photos.forEach { image ->
                writer.beginObject()
                writer.name("uri").value(image.uri.toString())
                writer.endObject()
            }
            writer.endArray()
            writer.endObject()
        }
        writer.endArray()
    }

    companion object {
        fun createModel(noteProperty: NoteProperty): NoteModel {
            // Close old session
            getKoin().getScopeOrNull(Constants.SCOPE_ID_NOTE)?.close()
            // Create new session
            val noteSession = getKoin()
                .getOrCreateScope(Constants.SCOPE_ID_NOTE, named(Constants.SCOPE_NAME_NOTE))
            val noteModel: NoteModel = noteSession.get()
            noteModel.init(noteProperty.fileName, noteProperty.title, noteProperty.subTitle)
            Log.d(TAG, "create NoteModel")
            return noteModel
        }

        private const val TAG = "NoteModel"
    }
}