package net.engawapg.app.camrepo.photo

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.navigation.fragment.navArgs
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import kotlinx.android.synthetic.main.fragment_photo_pager.*
import net.engawapg.app.camrepo.R
import org.koin.android.viewmodel.ext.android.sharedViewModel

class PhotoPagerFragment : Fragment() {

    private val args: PhotoPagerFragmentArgs by navArgs()
    private val viewModel: PhotoViewModel by sharedViewModel()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_photo_pager, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val pageIndex = args.PageIndex
        val photoIndex = args.PhotoIndex
        val wholeOfNote = args.WholeOfNote
        Log.d(TAG, "pageIndex=$pageIndex, photoIndex=$photoIndex, wholeOfNote=$wholeOfNote")

        /* ViewModel初期化 */
        if (wholeOfNote) {
            viewModel.initModel(-1) /* ノート全体 */
        } else {
            viewModel.initModel(pageIndex)
        }

        photoPager.registerOnPageChangeCallback(pageChangeCallback)
        photoPager.offscreenPageLimit = 1
        activity?.let {
            photoPager.adapter = PhotoAdapter(it, viewModel)
        }
        val position = viewModel.getPosition(pageIndex, photoIndex)
        photoPager.setCurrentItem(position, false)
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
        val aca = activity as AppCompatActivity?
        aca?.supportActionBar?.title = "${index + 1}/${viewModel.getPhotoCount()} (${viewModel.getTitle()})"
    }

    companion object {
        private const val TAG = "PhotoPagerFragment"
    }
}