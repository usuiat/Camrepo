package net.engawapg.app.camrepo.photo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import net.engawapg.app.camrepo.R
import org.koin.android.viewmodel.ext.android.viewModel

class PhotoActivity : AppCompatActivity() {

    private val viewModel: PhotoViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo)

        val pageIndex = intent.getIntExtra(KEY_PAGE_INDEX, 0)
        val photoIndex = intent.getIntExtra(KEY_PHOTO_INDEX, 0)
        val wholeOfNote = intent.getBooleanExtra(KEY_WHOLE_OF_NOTE, false)

        /* ViewModel初期化 */
        if (wholeOfNote) {
            viewModel.initModel(-1) /* ノート全体 */
        } else {
            viewModel.initModel(pageIndex)
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, PhotoPagerFragment.newInstance(pageIndex, photoIndex))
                .commitNow()
        }
    }


    companion object {
        const val KEY_PAGE_INDEX = "KeyPageIndex"
        const val KEY_PHOTO_INDEX = "KeyPhotoIndex"
        const val KEY_WHOLE_OF_NOTE = "KeyWholeOfNote"
    }
}