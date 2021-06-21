package net.engawapg.app.camrepo.page

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import net.engawapg.app.camrepo.model.ImageInfo
import net.engawapg.app.camrepo.model.NoteModel

class CameraViewModel(private val noteModel: NoteModel): ViewModel() {

    val eventAddImagePageIndex = MutableLiveData<Int>()
    var currentPageIndex: Int = 0       // 表示中のページインデックス
    private var pageIndexToAddImage: Int = 0    // シャッター押された時点でのページインデックス

    fun onPressShutter() {
        pageIndexToAddImage = currentPageIndex
        Log.d(TAG, "Page index to add image is $pageIndexToAddImage")
    }

    fun addImageInfo(info: ImageInfo) {
        viewModelScope.launch {
            noteModel.addPhotoAt(pageIndexToAddImage, info)
            eventAddImagePageIndex.value = pageIndexToAddImage
        }
    }

    companion object {
        private const val TAG = "CameraViewModel"
    }
}