package net.engawapg.app.camrepo.page

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import android.view.inputmethod.InputMethodManager
import androidx.lifecycle.Observer
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_page.*
import kotlinx.android.synthetic.main.view_page_memo.view.*
import kotlinx.android.synthetic.main.view_page_photo.view.*
import kotlinx.android.synthetic.main.view_page_title.view.*
import net.engawapg.app.camrepo.DeleteConfirmDialog
import net.engawapg.app.camrepo.R
import net.engawapg.app.camrepo.note.NoteViewModel
import org.koin.android.viewmodel.ext.android.getViewModel
import org.koin.android.viewmodel.ext.android.sharedViewModel
import org.koin.core.parameter.parametersOf

class PageFragment : Fragment() {

    private val args: PageFragmentArgs by navArgs()
    private lateinit var viewModel: PageViewModel
    private val noteViewModel: NoteViewModel by sharedViewModel()
    private val cameraViewModel: CameraViewModel by sharedViewModel()
    private var actionMode: ActionMode? = null
    private lateinit var pageItemAdapter: PageItemAdapter
    private var cameraFragmentId = 0
    private var pageIndex = 0
    private lateinit var inputMethodManager: InputMethodManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true) /* Toolbarにメニューあり */
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pageIndex = args.PageIndex
        Log.d(TAG, "PageIndex = $pageIndex")
        viewModel = getViewModel { parametersOf(pageIndex, IMAGE_SPAN_COUNT) }

        /* RecyclerView */
        pageItemAdapter = PageItemAdapter(viewModel, onFocusChangeListenerForRecyclerView) { position ->
            onItemClick( position )
        }

        recyclerView.apply {
            layoutManager = GridLayoutManager(context, IMAGE_SPAN_COUNT).apply {
                spanSizeLookup = PageItemSpanSizeLookup()
            }
            adapter = pageItemAdapter
        }
        itemTouchHelper.attachToRecyclerView(recyclerView)

        /* 写真追加イベントの監視 */
        cameraViewModel.eventAddImagePageIndex.observe(viewLifecycleOwner, Observer { index ->
            if (index == pageIndex) {
                pageItemAdapter.notifyDataSetChanged()
                recyclerView.scrollToPosition(viewModel.getItemCount(false) - 2)
                viewModel.modified = true
            }
        })

        /* 写真操作時にキーボードを閉じるためのやつ */
        inputMethodManager = activity?.getSystemService(Context.INPUT_METHOD_SERVICE)
                as InputMethodManager

        /* Observe result from DeleteConfirmDialog */
        findNavController().currentBackStackEntry?.savedStateHandle
            ?.getLiveData<Int>(DeleteConfirmDialog.KEY_RESULT)
            ?.observe(viewLifecycleOwner) { result ->
                onDeleteConfirmDialogResult(result)
            }
    }

    override fun onPause() {
        val modified = viewModel.modified
        viewModel.save()
        if (modified) {
            noteViewModel.pageModified.value = true
        }
        /* キーボード消す */
        inputMethodManager.hideSoftInputFromWindow(activity?.currentFocus?.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
        super.onPause()
    }

    private fun onItemClick(position: Int) {
        if (pageItemAdapter.getItemViewType(position) == PageViewModel.VIEW_TYPE_ADD_PHOTO) {
            showCameraFragment()
        } else if (pageItemAdapter.getItemViewType(position) == PageViewModel.VIEW_TYPE_PHOTO) {
            val photoIndex = viewModel.getPhotoIndexOfItemIndex(position, false)
            val action = PageFragmentDirections
                .actionPageFragmentToPhotoPagerFragment(pageIndex, photoIndex, false)
            findNavController().navigate(action)
        }
    }

    private val onFocusChangeListenerForRecyclerView = View.OnFocusChangeListener { v, hasFocus ->
        if (hasFocus &&
            ((v.tag == PageTitleViewHolder.TAG_PATE_TITLE) || (v.tag == MemoViewHolder.TAG_MEMO))) {
            hideCameraFragment()
        }
    }

    private fun showCameraFragment() {
        var cf = childFragmentManager.findFragmentById(cameraFragmentId)
        if (cf == null) {
            /* キーボード消す */
            inputMethodManager.hideSoftInputFromWindow(activity?.currentFocus?.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
            /* EditTextからフォーカスを外す */
            rootLayout.requestFocus()

            cf = CameraFragment.newInstance()
            childFragmentManager.beginTransaction()
                .add(R.id.cameraFragmentContainer, cf)
                .runOnCommit {
                    /* カメラアイコンが見えるようにスクロール */
                    recyclerView.scrollToPosition(viewModel.getItemCount(false) - 2)
                }
                .commit()
            cameraFragmentId = cf.id
            cameraViewModel.currentPageIndex = pageIndex
        }
    }

    private fun hideCameraFragment() {
        val cf = childFragmentManager.findFragmentById(cameraFragmentId)
        if (cf != null) {
            childFragmentManager.beginTransaction()
                .remove(cf)
                .commit()
            cameraFragmentId = 0
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_page, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.edit_list_items -> {
                hideCameraFragment()
                actionMode = activity?.startActionMode(actionModeCallback)
                true
            }
            R.id.slideshow -> {
//                startActivity(Intent(this, SlideshowActivity::class.java).apply {
//                    putExtra(SlideshowActivity.KEY_PAGE_INDEX, pageIndex)
//                })
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private val actionModeCallback = object: ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            mode?.menuInflater?.inflate(R.menu.menu_page_action_mode, menu)
            viewModel.initPhotoSelection()
            pageItemAdapter.setEditMode(true)
            itemTouchHelper.attachToRecyclerView(null)
            /* キーボード消す */
            inputMethodManager.hideSoftInputFromWindow(activity?.currentFocus?.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
            return true
        }

        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
            if (item?.itemId == R.id.delete_selected_items) {
                if (viewModel.isPhotoSelected()) {
                    findNavController().navigate(R.id.action_global_deleteConfirmDialog)
                }
            }
            return true
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            pageItemAdapter.setEditMode(false)
            itemTouchHelper.attachToRecyclerView(recyclerView)
            /* PageTitleにフォーカスが当たってしまうのを防ぐ */
            rootLayout.requestFocus()
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?) = false
    }

    private fun onDeleteConfirmDialogResult(result: Int) {
        Log.d(TAG, "onDeleteConfirmDialogResult: $result")
        if (result == DeleteConfirmDialog.RESULT_DELETE) {
            viewModel.deleteSelectedPhotos()
            actionMode?.finish()
        }
    }

    class PageItemAdapter(private val viewModel: PageViewModel,
                          private val onFocusChangeListener: View.OnFocusChangeListener,
                          private val onItemClick: ((Int)->Unit))
        : RecyclerView.Adapter<BaseViewHolder>() {

        private var editMode = false
        fun setEditMode(mode: Boolean) {
            editMode = mode
            notifyDataSetChanged()
        }

        override fun getItemCount() = viewModel.getItemCount(editMode)

        override fun getItemViewType(position: Int) = viewModel.getViewType(position, editMode)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
            return when (viewType) {
                PageViewModel.VIEW_TYPE_PAGE_TITLE -> PageTitleViewHolder.create(parent, viewModel, onFocusChangeListener)
                PageViewModel.VIEW_TYPE_PHOTO -> PhotoViewHolder.create(parent, viewModel)
                PageViewModel.VIEW_TYPE_ADD_PHOTO -> AddPhotoViewHolder.create(parent)
                PageViewModel.VIEW_TYPE_MEMO -> MemoViewHolder.create(parent, viewModel, onFocusChangeListener)
                else -> BaseViewHolder.create(parent)
            }
        }

        override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
            holder.bind(position, editMode)
            holder.itemView.setOnClickListener {
                onItemClick(holder.adapterPosition)
            }
        }
    }

    open class BaseViewHolder(v: View): RecyclerView.ViewHolder(v) {
        companion object {
            fun create(parent: ViewGroup): BaseViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val view = layoutInflater.inflate(R.layout.view_note_blank, parent, false)
                return BaseViewHolder(view)
            }
        }

        open fun bind(position: Int, editMode: Boolean) {}
    }

    class PageTitleViewHolder(v: View,
                              private val viewModel: PageViewModel,
                              private val onFocusChangeListener: View.OnFocusChangeListener)
        : BaseViewHolder(v) {

        companion object {
            fun create(parent: ViewGroup,
                       viewModel: PageViewModel,
                       onFocusChangeListener: View.OnFocusChangeListener): PageTitleViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val view = layoutInflater.inflate(R.layout.view_page_title, parent, false)
                return PageTitleViewHolder(view, viewModel, onFocusChangeListener)
            }
            const val TAG_PATE_TITLE = "TagPageTitle"
        }

        override fun bind(position: Int, editMode: Boolean) {
            itemView.pageTitle.apply {
                setText(viewModel.getPageTitle())
                isEnabled = !editMode
                addTextChangedListener(object: TextWatcher {
                    override fun afterTextChanged(s: Editable?) {
                        viewModel.setPageTitle(s.toString())
                    }
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int ) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                })
                onFocusChangeListener = this@PageTitleViewHolder.onFocusChangeListener
                tag = TAG_PATE_TITLE
            }
        }
    }

    class PhotoViewHolder(v: View, private val viewModel: PageViewModel): BaseViewHolder(v) {

        companion object {
            fun create(parent: ViewGroup, viewModel: PageViewModel): PhotoViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val view = layoutInflater.inflate(R.layout.view_page_photo, parent, false)
                return PhotoViewHolder(view, viewModel)
            }
        }

        override fun bind(position: Int, editMode: Boolean) {
            val photoIndex = viewModel.getPhotoIndexOfItemIndex(position, editMode)
            val resolver = itemView.context.contentResolver
            val bmp = viewModel.getPhotoBitmap(photoIndex, resolver)

            if (bmp != null) {
                itemView.imageView.setImageBitmap(bmp)
            } else {
                itemView.imageView.setImageResource(R.drawable.imagenotfound)
            }

            itemView.checkBox.apply {
                visibility = if (editMode) View.VISIBLE else View.INVISIBLE
                isChecked = viewModel.getPhotoSelection(position)
                setOnClickListener {
                    viewModel.setPhotoSelection(adapterPosition, isChecked)
                }
            }
        }
    }

    class AddPhotoViewHolder(v: View): BaseViewHolder(v) {

        companion object {
            fun create(parent: ViewGroup): AddPhotoViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val view = layoutInflater.inflate(R.layout.view_page_add_photo, parent, false)
                return AddPhotoViewHolder(view)
            }
        }
    }

    class MemoViewHolder(v: View, private val viewModel: PageViewModel,
                         private val onFocusChangeListener: View.OnFocusChangeListener)
        : BaseViewHolder(v) {

        companion object {
            fun create(parent: ViewGroup, viewModel: PageViewModel,
                       onFocusChangeListener: View.OnFocusChangeListener): MemoViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val view = layoutInflater.inflate(R.layout.view_page_memo, parent, false)
                return MemoViewHolder(view, viewModel, onFocusChangeListener)
            }
            const val TAG_MEMO = "TagMemo"
        }

        override fun bind(position: Int, editMode: Boolean) {
            itemView.memo.apply {
                setText(viewModel.getMemo())
                isEnabled = !editMode
                addTextChangedListener(object: TextWatcher {
                    override fun afterTextChanged(s: Editable?) {
                        viewModel.setMemo(s.toString())
                    }
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int ) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                })
                onFocusChangeListener = this@MemoViewHolder.onFocusChangeListener
                tag = TAG_MEMO
            }
        }
    }

    inner class PageItemSpanSizeLookup: GridLayoutManager.SpanSizeLookup() {
        override fun getSpanSize(position: Int): Int {
            return when (pageItemAdapter.getItemViewType(position)) {
                PageViewModel.VIEW_TYPE_PAGE_TITLE -> IMAGE_SPAN_COUNT
                PageViewModel.VIEW_TYPE_MEMO -> IMAGE_SPAN_COUNT
                else -> 1
            }
        }
    }

    private val itemTouchHelper = ItemTouchHelper(object: ItemTouchHelper.SimpleCallback(
        0, 0
    ) {
        override fun getMovementFlags(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder
        ): Int {
            if (viewHolder is PhotoViewHolder) {
                val drag = ItemTouchHelper.UP or ItemTouchHelper.DOWN or
                        ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
                return ItemTouchHelper.Callback.makeMovementFlags(drag, 0)
            }
            return 0
        }

        override fun canDropOver(
            recyclerView: RecyclerView,
            current: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            return (target is PhotoViewHolder)
        }

        override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
            super.onSelectedChanged(viewHolder, actionState)
            Log.d(TAG, "ItemTouchHelper onSelectedChanged")
            inputMethodManager.hideSoftInputFromWindow(activity?.currentFocus?.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
            rootLayout.requestFocus()
        }

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            val from = viewHolder.adapterPosition
            val to = target.adapterPosition
            viewModel.movePhoto(from - 1, to - 1)   /* from/toからPage Title分を除外 */
            pageItemAdapter.notifyItemMoved(from, to)
            return true
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
    })

    companion object {
        private const val TAG = "PageFragment"
        private const val IMAGE_SPAN_COUNT = 4
//        @JvmStatic
//        fun newInstance() = PageFragment()
    }
}