package net.engawapg.app.camrepo.slideshow

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_slideshow.*
import net.engawapg.app.camrepo.R
import org.koin.android.viewmodel.ext.android.sharedViewModel

private const val ARG_PAGE_INDEX = "ArgPageIndex"

/**
 * A simple [Fragment] subclass.
 * Use the [SlideshowFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class SlideshowFragment : Fragment() {
    private val viewModel: SlideshowViewModel by sharedViewModel()
    private var pageIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            pageIndex = it.getInt(ARG_PAGE_INDEX, 0)
        }
        Log.d(TAG, "pageIndex = $pageIndex")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_slideshow, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        slideWebView.loadDataWithBaseURL(
            ASSET_URL, viewModel.getHtml(pageIndex),
            "text/html", "UTF-8", null
        )
    }

    companion object {
        @JvmStatic
        fun newInstance(pageIndex: Int) =
            SlideshowFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_PAGE_INDEX, pageIndex)
                }
            }

        private const val TAG = "SlideshowFragment"
        private const val ASSET_URL = "file:///android_asset/"
    }
}