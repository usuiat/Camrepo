package net.engawapg.app.camrepo.page

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import net.engawapg.app.camrepo.R

class PageFragment : Fragment() {

    private var pageIndex: Int = 0
    private lateinit var viewModel: PageViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            pageIndex = it.getInt(ARG_PAGE_INDEX)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.page_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
    }

    companion object {
        fun newInstance(index: Int) = PageFragment().apply {
            arguments = Bundle().apply {
                putInt(ARG_PAGE_INDEX, index)
            }
        }

        private const val ARG_PAGE_INDEX = "ArgPageIndex"
    }
}