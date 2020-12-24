package net.engawapg.app.camrepo.page

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import net.engawapg.app.camrepo.R

class PageFragment : Fragment() {

    private val args: PageFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val index = args.PageIndex
        Log.d(TAG, "PageIndex = $index")
    }

    companion object {
        private const val TAG = "PageFragment"
//        @JvmStatic
//        fun newInstance() = PageFragment()
    }
}