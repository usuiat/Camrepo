package net.engawapg.app.camrepo.page

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.page_fragment.*
import net.engawapg.app.camrepo.R

class PageFragment : Fragment() {

    private var pageIndex: Int = 0
    private lateinit var adapter: PageItemAdapter
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pageRecyclerView.layoutManager = GridLayoutManager(context, IMAGE_SPAN_COUNT).apply {
            spanSizeLookup = object: GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int) =
                    if (adapter.getItemViewType(position) == PageItemAdapter.VIEW_TYPE_PHOTO) 1
                    else IMAGE_SPAN_COUNT
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        adapter = PageItemAdapter()
        pageRecyclerView.adapter = adapter
    }

    class PageItemAdapter: RecyclerView.Adapter<BaseViewHolder>() {
        override fun getItemCount(): Int {
            return 9
        }

        override fun getItemViewType(position: Int): Int {
            return when (position) {
                0 -> VIEW_TYPE_TITLE
                itemCount - 1 -> VIEW_TYPE_MEMO
                else -> VIEW_TYPE_PHOTO
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
            return when (viewType) {
                VIEW_TYPE_TITLE -> TitleViewHolder.create(parent)
                VIEW_TYPE_PHOTO -> PhotoViewHolder.create(parent)
                else -> MemoViewHolder.create(parent)
            }
        }

        override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
            holder.bind(position)
        }

        companion object {
            const val VIEW_TYPE_TITLE = 1
            const val VIEW_TYPE_PHOTO = 2
            const val VIEW_TYPE_MEMO = 3
        }
    }

    open class BaseViewHolder(v: View): RecyclerView.ViewHolder(v) {
        open fun bind(position: Int) {}
    }

    class TitleViewHolder(v: View): BaseViewHolder(v) {
        companion object {
            fun create(parent: ViewGroup): TitleViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val view = layoutInflater.inflate(R.layout.view_page_title, parent, false)
                return TitleViewHolder(view)
            }
        }
    }

    class PhotoViewHolder(v: View): BaseViewHolder(v) {
        companion object {
            fun create(parent: ViewGroup): PhotoViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val view = layoutInflater.inflate(R.layout.view_page_photo, parent, false)
                return PhotoViewHolder(view)
            }
        }
    }

    class MemoViewHolder(v: View): BaseViewHolder(v) {
        companion object {
            fun create(parent: ViewGroup): MemoViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val view = layoutInflater.inflate(R.layout.view_page_memo, parent, false)
                return MemoViewHolder(view)
            }
        }
    }

    companion object {
        fun newInstance(index: Int) = PageFragment().apply {
            arguments = Bundle().apply {
                putInt(ARG_PAGE_INDEX, index)
            }
        }

        private const val ARG_PAGE_INDEX = "ArgPageIndex"
        private const val IMAGE_SPAN_COUNT = 4
    }
}