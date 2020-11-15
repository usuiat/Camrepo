package net.engawapg.app.camrepo.slideshow

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import net.engawapg.app.camrepo.R

private const val ARG_PAGE_INDEX = "ArgPageIndex"

/**
 * A simple [Fragment] subclass.
 * Use the [SlideshowFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class SlideshowFragment : Fragment() {
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

    companion object {
        @JvmStatic
        fun newInstance(pageIndex: Int) =
            SlideshowFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_PAGE_INDEX, pageIndex)
                }
            }

        private const val TAG = "SlideshowFragment"
    }
}