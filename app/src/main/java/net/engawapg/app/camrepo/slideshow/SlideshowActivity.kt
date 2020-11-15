package net.engawapg.app.camrepo.slideshow

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import kotlinx.android.synthetic.main.activity_page.toolbar
import kotlinx.android.synthetic.main.activity_slideshow.*
import net.engawapg.app.camrepo.R
import org.koin.android.viewmodel.ext.android.viewModel

class SlideshowActivity : AppCompatActivity() {

    private val viewModel: SlideshowViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_slideshow)

        /* Get page index */
        val pageIndex = intent.getIntExtra(KEY_PAGE_INDEX, 0)

        /* ToolBar */
        setSupportActionBar(toolbar)
        supportActionBar?.let {
            it.setDisplayHomeAsUpEnabled(true)
            it.setHomeButtonEnabled(true)
            it.title = ""
        }

        /* Pager */
        slidePager.apply {
            offscreenPageLimit = 1
            adapter = SlideAdapter(this@SlideshowActivity, viewModel)
            setCurrentItem(pageIndex, false)
        }
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

    class SlideAdapter(fa: FragmentActivity, private val viewModel: SlideshowViewModel)
        : FragmentStateAdapter(fa) {
        override fun getItemCount() = viewModel.getSlideCount()
        override fun createFragment(position: Int): Fragment {
            return SlideshowFragment.newInstance(position)
        }
    }

    companion object {
        const val KEY_PAGE_INDEX = "KeyPageIndex"
    }
}