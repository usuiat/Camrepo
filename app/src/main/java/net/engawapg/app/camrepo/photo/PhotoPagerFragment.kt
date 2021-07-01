package net.engawapg.app.camrepo.photo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import net.engawapg.app.camrepo.databinding.FragmentPhotoPagerBinding
import org.koin.android.viewmodel.ViewModelOwner.Companion.from
import org.koin.android.viewmodel.ext.android.sharedViewModel

class PhotoPagerFragment: Fragment() {

    private val args: PhotoPagerFragmentArgs by navArgs()
    private var _binding: FragmentPhotoPagerBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PhotoViewModel by sharedViewModel(owner = {from(this)})

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPhotoPagerBinding.inflate(inflater, container, false)

        /* ViewModel初期化 */
        if (args.wholeOfNote) {
            viewModel.initModel(-1) /* ノート全体 */
        } else {
            viewModel.initModel(args.pageIndex)
        }

        val position = viewModel.getPosition(args.pageIndex, args.photoIndex)

        binding.photoPager.registerOnPageChangeCallback(pageChangeCallback)
        binding.photoPager.offscreenPageLimit = 1
        binding.photoPager.adapter = PhotoAdapter(this, viewModel)
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

    class PhotoAdapter(fragment: Fragment, private val viewModel: PhotoViewModel)
        : FragmentStateAdapter(fragment) {

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