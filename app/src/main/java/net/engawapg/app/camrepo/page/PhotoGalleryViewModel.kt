package net.engawapg.app.camrepo.page

import android.app.Application
import android.content.ContentUris
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.engawapg.app.camrepo.util.Event

class PhotoGalleryViewModel(app: Application) : AndroidViewModel(app) {
    /* Permission承認されたかどうか */
    val isPermissionGranted = MutableLiveData<Boolean>().apply { value = false }
    /* Permission拒否されたかどうか */
    val isPermissionDenied = MutableLiveData<Boolean>().apply { value = false }
    /* 写真のリスト */
    val photoList = MutableLiveData<List<PhotoGalleryItem>>()
    /* 写真選択イベント */
    val onSelect = MutableLiveData<Event<PhotoGalleryItem>>()

    /* 写真のリストを読み込む */
    fun loadPhotoList() {
        /* IOスレッドでMediaStoreから写真を検索する */
        viewModelScope.launch(Dispatchers.IO) {
            val list = mutableListOf<PhotoGalleryItem>()

            /* 読み込む列の指定 */
            val projection = arrayOf(
                MediaStore.Images.Media._ID, /* ID : URI取得に必要 */
                MediaStore.Images.Media.DATE_ADDED  /* 追加日時 */
            )
            val selection = null /* 行の絞り込みの指定。nullならすべての行を読み込む。*/
            val selectionArgs = null /* selectionの?を置き換える引数 */
            /* 並び順の指定 : 追加日時の新しい順 */
            val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

            getApplication<Application>().contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection, selection, selectionArgs, sortOrder
            )?.use { cursor -> /* cursorは、検索結果の各行の情報にアクセスするためのオブジェクト。*/
                /* 必要な情報が格納されている列番号を取得する。 */
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                while (cursor.moveToNext()) { /* 順にカーソルを動かしながら、情報を取得していく。*/
                    val id = cursor.getLong(idColumn)
                    /* IDからURIを取得してリストに格納 */
                    val uri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                    list.add(PhotoGalleryItem(uri))
                }
            }

            /* メインスレッドでphotoListにセットする */
            photoList.postValue(list)
        }
    }

    /* 指定したインデックスのPhotoGalleryItemを取得 */
    fun getPhotoItem(index: Int) = photoList.value?.getOrNull(index)

    /* 写真クリックイベント */
    fun onClick(item: PhotoGalleryItem) {
        /* URIをイベントとして渡す */
        onSelect.value = Event(item)
    }
}