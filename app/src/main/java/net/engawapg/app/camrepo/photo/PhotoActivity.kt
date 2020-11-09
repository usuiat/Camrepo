package net.engawapg.app.camrepo.photo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import kotlinx.android.synthetic.main.activity_photo.*
import net.engawapg.app.camrepo.R
import org.koin.android.viewmodel.ext.android.getViewModel
import org.koin.core.parameter.parametersOf

class PhotoActivity : AppCompatActivity() {

    private lateinit var viewModel: PhotoViewModel
    private var pageIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo)

        pageIndex = intent.getIntExtra(KEY_PAGE_INDEX, 0)
        val photoIndex = intent.getIntExtra(KEY_PHOTO_INDEX, 0)
        viewModel = getViewModel { parametersOf(pageIndex) }
        Log.d(TAG, "pageIndex = $pageIndex")
        Log.d(TAG, "viewModel = $viewModel")

        /* ToolBar */
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeButtonEnabled(true)
            title = ""
        }

        photoPager.offscreenPageLimit = 1
        photoPager.adapter = PhotoAdapter(this, viewModel)
        photoPager.setCurrentItem(photoIndex, false)
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

    companion object {
        const val KEY_PAGE_INDEX = "KeyPageIndex"
        const val KEY_PHOTO_INDEX = "KeyPhotoIndex"
        private const val TAG = "PhotoActivity"
    }
}