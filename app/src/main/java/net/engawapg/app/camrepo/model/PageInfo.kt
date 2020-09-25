package net.engawapg.app.camrepo.model

class PageInfo {
    var title = ""
    var memo = ""
    var photos = listOf<ImageInfo>()

    fun getPhotoAt(index: Int) = photos.getOrNull(index)

    fun addPhoto(imageInfo: ImageInfo) {
        photos = photos.plus(imageInfo)
    }

    fun deletePhotosAt(indexes: List<Int>) {
        photos = photos.filterIndexed { index, _ -> !indexes.contains(index) }
    }

    fun movePhoto(from: Int, to: Int) {
        val mPhotos = photos.toMutableList()
        val info = mPhotos.removeAt(from)
        mPhotos.add(to, info)
        photos = mPhotos
    }
}