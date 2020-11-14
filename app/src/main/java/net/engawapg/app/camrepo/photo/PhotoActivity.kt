package net.engawapg.app.camrepo.photo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import kotlinx.android.synthetic.main.activity_photo.*
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
        Log.d(TAG, "pageIndex = $pageIndex")
        Log.d(TAG, "viewModel = $viewModel")

        /* ViewModel初期化 */
        if (wholeOfNote) {
            viewModel.initModel(-1) /* ノート全体 */
        } else {
            viewModel.initModel(pageIndex)
        }

        /* ToolBar */
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeButtonEnabled(true)
            title = ""
        }

        photoPager.registerOnPageChangeCallback(pageChangeCallback)
        photoPager.offscreenPageLimit = 1
        photoPager.adapter = PhotoAdapter(this, viewModel)
        val position = viewModel.getPosition(pageIndex, photoIndex)
        photoPager.setCurrentItem(position, false)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    class PhotoAdapter(fa: FragmentActivity, private val viewModel: PhotoViewModel)
        : FragmentStateAdapter(fa) {

        override fun getItemCount() = viewModel.getPhotoCount()
        override fun createFragment(position: Int): Fragment {
            return PhotoFragment.newInstance(position)
        }
    }

    private val pageChangeCallback = object: ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            showToolbarTitle(position)
        }
    }

    /** Toolbarにタイトルを表示
     * @param index: 0始まりの写真の番号
     */
    private fun showToolbarTitle(index: Int) {
        supportActionBar?.title = "${index + 1}/${viewModel.getPhotoCount()} (${viewModel.getTitle()})"
    }

    companion object {
        const val KEY_PAGE_INDEX = "KeyPageIndex"
        const val KEY_PHOTO_INDEX = "KeyPhotoIndex"
        const val KEY_WHOLE_OF_NOTE = "KeyWholeOfNote"
        private const val TAG = "PhotoActivity"
    }
}