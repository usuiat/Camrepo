package net.engawapg.app.camrepo.page

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import net.engawapg.app.camrepo.R
import org.koin.android.viewmodel.ext.android.getViewModel
import org.koin.core.parameter.parametersOf

class PageActivity : AppCompatActivity() {

    private lateinit var viewModel: PageViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_page)

        /* Get PageIndex */
        val pageIndex = intent.getIntExtra(KEY_PAGE_INDEX, 0)
        viewModel = getViewModel { parametersOf(pageIndex, PageFragment.IMAGE_SPAN_COUNT) }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, PageFragment.newInstance())
                .commitNow()
        }
    }

    companion object {
        const val KEY_PAGE_INDEX = "KeyPageIndex"
    }
}