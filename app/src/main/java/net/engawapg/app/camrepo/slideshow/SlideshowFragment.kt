package net.engawapg.app.camrepo.slideshow

import android.graphics.ImageDecoder
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_slideshow.*
import kotlinx.android.synthetic.main.view_slide_photo_grid.view.*
import net.engawapg.app.camrepo.R
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
    private val viewModel: SlideViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            viewModel.pageIndex = it.getInt(ARG_PAGE_INDEX, 0)
        }
        Log.d(TAG, "pageIndex = ${viewModel.pageIndex}")
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
        textView_title.text = viewModel.getPageTitle()

        val memo = viewModel.getPageMemo()
        val isMemoEmpty = memo.isEmpty()
        if (isMemoEmpty) {
            group_memo.visibility = View.GONE
        } else {
            group_memo.visibility = View.VISIBLE
            textView_memo.text = memo
        }

        val photoCount = viewModel.getPhotoCount()
        if (photoCount == 0) {
            photoGrid.visibility = View.GONE
        } else {
            photoGrid.visibility = View.VISIBLE
            val span = getPhotoGridSpan(isMemoEmpty)
            photoGrid.layoutManager = GridLayoutManager(context, span)
            photoGrid.adapter = PhotoGridAdapter(viewModel)
        }
    }

    private fun getPhotoGridSpan(isFullScreen: Boolean): Int {
        /* 写真数を取得 */
        val photoCount = viewModel.getPhotoCount()
        /* 写真の最大数を制限 */
        val count = if (isFullScreen) {
            if (photoCount < 50) photoCount else 50
        } else {
            if (photoCount < 25) photoCount else 25
        }

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
        Log.d(TAG, "Photo count = $photoCount, Span = $span")

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

    class PhotoGridAdapter(private val viewModel: SlideViewModel)
        : RecyclerView.Adapter<PhotoGridViewHolder>() {

        override fun getItemCount() = viewModel.getPhotoCount()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoGridViewHolder {
            val layoutInflater = LayoutInflater.from(parent.context)
            val view = layoutInflater.inflate(R.layout.view_slide_photo_grid, parent, false)
            return PhotoGridViewHolder(view)
        }

        override fun onBindViewHolder(holder: PhotoGridViewHolder, position: Int) {
            val imageInfo = viewModel.getPhoto(position)
            val resolver = holder.itemView.context.contentResolver
            val bmp = try {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                    MediaStore.Images.Media.getBitmap(resolver, imageInfo?.uri)
                } else {
                    val decoder = ImageDecoder.createSource(resolver!!, imageInfo!!.uri)
                    ImageDecoder.decodeBitmap(decoder)
                }
            } catch (e: Exception) {
                null
            }
            if (bmp != null) {
                holder.itemView.imageView.setImageBitmap(bmp)
            } else {
                holder.itemView.imageView.setImageResource(R.drawable.imagenotfound)
            }
        }
    }

    class PhotoGridViewHolder(v: View): RecyclerView.ViewHolder(v) {

    }
}