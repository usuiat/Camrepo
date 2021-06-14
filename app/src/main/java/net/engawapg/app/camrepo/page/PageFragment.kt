package net.engawapg.app.camrepo.page

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import net.engawapg.app.camrepo.DeleteConfirmDialog
import net.engawapg.app.camrepo.R
import net.engawapg.app.camrepo.databinding.FragmentPageBinding
import net.engawapg.app.camrepo.databinding.ViewPageMemoBinding
import net.engawapg.app.camrepo.databinding.ViewPagePhotoBinding
import net.engawapg.app.camrepo.databinding.ViewPageTitleBinding
import org.koin.android.viewmodel.ViewModelOwner.Companion.from
import org.koin.android.viewmodel.ext.android.getViewModel
import org.koin.android.viewmodel.ext.android.sharedViewModel
import org.koin.core.parameter.parametersOf

class PageFragment: Fragment(), DeleteConfirmDialog.EventListener {

    companion object {
        const val IMAGE_SPAN_COUNT = 4
        private const val DELETE_CONFIRM_DIALOG = "DeleteConfirmDialog"
    }

    private val args: PageFragmentArgs by navArgs()
    private var _binding: FragmentPageBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: PageViewModel
    private val cameraViewModel: CameraViewModel by sharedViewModel(owner = { from(this)} )
    private var actionMode: ActionMode? = null
    private lateinit var pageItemAdapter: PageItemAdapter
    private var cameraFragmentId = 0
    private lateinit var inputMethodManager: InputMethodManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPageBinding.inflate(inflater, container, false)

        viewModel = getViewModel { parametersOf(args.pageIndex, IMAGE_SPAN_COUNT) }

        /* RecyclerView */
        pageItemAdapter = PageItemAdapter(
            viewModel, onFocusChangeListenerForRecyclerView
        ) { position ->
            onItemClick(position)
        }

        binding.recyclerView.apply {
            layoutManager = GridLayoutManager(context, IMAGE_SPAN_COUNT).apply {
                spanSizeLookup = PageItemSpanSizeLookup()
            }
            adapter = pageItemAdapter
        }
        itemTouchHelper.attachToRecyclerView(binding.recyclerView)

        /* 写真追加イベントの監視 */
        cameraViewModel.eventAddImagePageIndex.observe(viewLifecycleOwner, { index ->
            if (index == viewModel.pageIndex) {
                pageItemAdapter.notifyDataSetChanged()
                binding.recyclerView.scrollToPosition(viewModel.getItemCount(false) - 2)
                viewModel.modified = true
            }
        })

        /* 写真操作時にキーボードを閉じるためのやつ */
        inputMethodManager = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onPause() {
        viewModel.save()
        super.onPause()
    }

    private fun onItemClick(position: Int) {
        if (pageItemAdapter.getItemViewType(position) == PageViewModel.VIEW_TYPE_ADD_PHOTO) {
            showCameraFragment()
        } else if (pageItemAdapter.getItemViewType(position) == PageViewModel.VIEW_TYPE_PHOTO) {
            val action = PageFragmentDirections.actionPageFragmentToPhotoPagerFragment(
                pageIndex = viewModel.pageIndex,
                photoIndex = viewModel.getPhotoIndexOfItemIndex(position, false)
            )
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
            inputMethodManager.hideSoftInputFromWindow(activity?.currentFocus?.windowToken,
                InputMethodManager.HIDE_NOT_ALWAYS)
            /* EditTextからフォーカスを外す */
            binding.root.requestFocus()

            cf = CameraFragment.newInstance()
            childFragmentManager.beginTransaction()
                .add(R.id.cameraFragmentContainer, cf)
                .runOnCommit {
                    /* カメラアイコンが見えるようにスクロール */
                    binding.recyclerView.scrollToPosition(viewModel.getItemCount(false) - 2)
                }
                .commit()
            cameraFragmentId = cf.id
            cameraViewModel.currentPageIndex = viewModel.pageIndex
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
                val action = PageFragmentDirections.actionPageFragmentToSlideshowActivity(
                    viewModel.pageIndex
                )
                findNavController().navigate(action)
                true
            }
            android.R.id.home -> {
                activity?.finish()
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
            inputMethodManager.hideSoftInputFromWindow(activity?.currentFocus?.windowToken,
                InputMethodManager.HIDE_NOT_ALWAYS)
            return true
        }

        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
            if (item?.itemId == R.id.delete_selected_items) {
                if (viewModel.isPhotoSelected()) {
                    DeleteConfirmDialog().show(childFragmentManager, DELETE_CONFIRM_DIALOG)
                }
            }
            return true
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            pageItemAdapter.setEditMode(false)
            itemTouchHelper.attachToRecyclerView(binding.recyclerView)
            /* PageTitleにフォーカスが当たってしまうのを防ぐ */
            binding.root.requestFocus()
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?) = false
    }

    override fun onClickDeleteButton() {
        viewModel.deleteSelectedPhotos()
        actionMode?.finish()
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

    class PageTitleViewHolder(private val binding: ViewPageTitleBinding,
                              private val viewModel: PageViewModel,
                              private val onFocusChangeListener: View.OnFocusChangeListener)
        : BaseViewHolder(binding.root) {

        companion object {
            fun create(parent: ViewGroup,
                       viewModel: PageViewModel,
                       onFocusChangeListener: View.OnFocusChangeListener): PageTitleViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ViewPageTitleBinding.inflate(layoutInflater, parent, false)
                return PageTitleViewHolder(binding, viewModel, onFocusChangeListener)
            }
            const val TAG_PATE_TITLE = "TagPageTitle"
        }

        override fun bind(position: Int, editMode: Boolean) {
            binding.pageTitle.apply {
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

    class PhotoViewHolder(private val binding: ViewPagePhotoBinding,
                          private val viewModel: PageViewModel): BaseViewHolder(binding.root) {

        companion object {
            fun create(parent: ViewGroup, viewModel: PageViewModel): PhotoViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ViewPagePhotoBinding.inflate(layoutInflater, parent, false)
                return PhotoViewHolder(binding, viewModel)
            }
        }

        override fun bind(position: Int, editMode: Boolean) {
            val photoIndex = viewModel.getPhotoIndexOfItemIndex(position, editMode)
            val resolver = itemView.context.contentResolver
            val bmp = viewModel.getPhotoBitmap(photoIndex, resolver)

            if (bmp != null) {
                binding.imageView.setImageBitmap(bmp)
            } else {
                binding.imageView.setImageResource(R.drawable.imagenotfound)
            }

            binding.checkBox.apply {
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

    class MemoViewHolder(private val binding: ViewPageMemoBinding,
                         private val viewModel: PageViewModel,
                         private val onFocusChangeListener: View.OnFocusChangeListener)
        : BaseViewHolder(binding.root) {

        companion object {
            fun create(parent: ViewGroup, viewModel: PageViewModel,
                       onFocusChangeListener: View.OnFocusChangeListener): MemoViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ViewPageMemoBinding.inflate(layoutInflater, parent, false)
                return MemoViewHolder(binding, viewModel, onFocusChangeListener)
            }
            const val TAG_MEMO = "TagMemo"
        }

        override fun bind(position: Int, editMode: Boolean) {
            binding.memo.apply {
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
                return makeMovementFlags(drag, 0)
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
            Log.d("PageFragment", "ItemTouchHelper onSelectedChanged")
            inputMethodManager.hideSoftInputFromWindow(activity?.currentFocus?.windowToken,
                InputMethodManager.HIDE_NOT_ALWAYS)
            binding.root.requestFocus()
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
}