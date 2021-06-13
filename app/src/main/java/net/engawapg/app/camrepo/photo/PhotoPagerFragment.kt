package net.engawapg.app.camrepo.photo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import net.engawapg.app.camrepo.databinding.FragmentPhotoPagerBinding
import org.koin.android.viewmodel.ext.android.sharedViewModel

private const val ARG_PAGE_INDEX = "pageIndex"
private const val ARG_PHOTO_INDEX = "photoIndex"

class PhotoPagerFragment: Fragment() {

    companion object {
        fun newInstance(pageIndex: Int, photoIndex: Int) = PhotoPagerFragment().apply {
            arguments = Bundle().apply {
                putInt(ARG_PAGE_INDEX, pageIndex)
                putInt(ARG_PHOTO_INDEX, photoIndex)
            }
        }
    }

    private var _binding: FragmentPhotoPagerBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PhotoViewModel by sharedViewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPhotoPagerBinding.inflate(inflater, container, false)

        val pageIndex = arguments?.run { getInt(ARG_PAGE_INDEX, 0) } ?: 0
        val photoIndex = arguments?.run { getInt(ARG_PHOTO_INDEX, 0) } ?: 0
        val position = viewModel.getPosition(pageIndex, photoIndex)

        binding.photoPager.registerOnPageChangeCallback(pageChangeCallback)
        binding.photoPager.offscreenPageLimit = 1
        binding.photoPager.adapter = PhotoAdapter(requireActivity(), viewModel)
        binding.photoPager.setCurrentItem(position, false)

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        showToolbarTitle(binding.photoPager.currentItem)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            android.R.id.home -> {
                activity?.finish()
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
        activity?.title = "${index + 1}/${viewModel.getPhotoCount()} (${viewModel.getTitle()})"
    }
}