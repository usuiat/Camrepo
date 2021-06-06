package net.engawapg.app.camrepo.slideshow

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import net.engawapg.app.camrepo.R
import net.engawapg.app.camrepo.databinding.FragmentSlideshowBinding
import net.engawapg.app.camrepo.databinding.ViewSlidePhotoGridBinding
import org.koin.android.viewmodel.ext.android.viewModel
import kotlin.math.ceil
import kotlin.math.sqrt

private const val ARG_PAGE_INDEX = "ArgPageIndex"

/**
 * A simple [Fragment] subclass.
 * Use the [SlideshowFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class SlideshowFragment : Fragment() {
    private var _binding: FragmentSlideshowBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SlideViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            viewModel.pageIndex = it.getInt(ARG_PAGE_INDEX, 0)
        }
        Log.d(TAG, "Slide page = ${viewModel.pageIndex}, title = ${viewModel.getPageTitle()}")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSlideshowBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.textViewTitle.text = viewModel.getPageTitle()

        val memo = viewModel.getPageMemo()
        val isMemoEmpty = memo.isEmpty()
        if (isMemoEmpty) {
            binding.groupMemo.visibility = View.GONE
        } else {
            binding.groupMemo.visibility = View.VISIBLE
            binding.textViewMemo.text = memo
        }

        val photoCount = viewModel.getPhotoCount()
        if (photoCount == 0) {
            binding.photoGrid.visibility = View.GONE
        } else {
            if ((photoCount == 1) and isMemoEmpty) {
                /* 写真が1枚だけの時は両側にスペースを配置して中央寄せにする */
                binding.groupSpacePhoto.visibility = View.VISIBLE
            } else {
                binding.groupSpacePhoto.visibility = View.GONE
            }
            binding.photoGrid.visibility = View.VISIBLE
            val span = getPhotoGridSpan(isMemoEmpty)
            binding.photoGrid.layoutManager = GridLayoutManager(context, span)
            binding.photoGrid.adapter = PhotoGridAdapter(viewModel, viewLifecycleOwner, span)
        }
    }

    private fun getPhotoGridSpan(isFullScreen: Boolean): Int {
        /* 写真数を取得 */
        val count = viewModel.getPhotoCount()

        /* 列数を決定 */
        val span = if (isFullScreen) {
            /* 縦横比 1:2 */
            when {
                count <= 4 -> count
                count <= 8 -> 4
                count <= 10 -> 5
                count <= 18 -> 6
                count <= 21 -> 7
                count <= 32 -> 8
                count <= 36 -> 9
                else -> 10
            }
        } else {
            /* 縦横比 1:1 */
            ceil(sqrt(count.toFloat())).toInt()
        }
        Log.d(TAG, "Photo count = $count, Span = $span")

        return span
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

    class PhotoGridAdapter(private val viewModel: SlideViewModel,
                           private val lifecycleOwner: LifecycleOwner, private val span: Int)
        : RecyclerView.Adapter<PhotoGridViewHolder>() {

        override fun getItemCount() = viewModel.getPhotoCount()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoGridViewHolder {
            val layoutInflater = LayoutInflater.from(parent.context)
            val binding = ViewSlidePhotoGridBinding.inflate(layoutInflater, parent, false)
            val width = parent.width / span
            Log.d(TAG, "PhotoGrid width = $width")
            return PhotoGridViewHolder(binding, width)
        }

        override fun onBindViewHolder(holder: PhotoGridViewHolder, position: Int) {
            val imageInfo = viewModel.getPhoto(position)
            lifecycleOwner.lifecycleScope.launch {
                val resolver = holder.itemView.context.contentResolver
                val bmp = resolver?.let { imageInfo?.getBitmapWithResolver(it, holder.size) }
                if (bmp != null) {
                    holder.binding.imageView.setImageBitmap(bmp)
                } else {
                    holder.binding.imageView.setImageResource(R.drawable.imagenotfound)
                }
            }
        }
    }

    class PhotoGridViewHolder(val binding: ViewSlidePhotoGridBinding, val size: Int)
        : RecyclerView.ViewHolder(binding.root)

}