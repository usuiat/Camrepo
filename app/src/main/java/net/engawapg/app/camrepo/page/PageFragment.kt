package net.engawapg.app.camrepo.page

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import net.engawapg.app.camrepo.BR
import net.engawapg.app.camrepo.DeleteConfirmDialog
import net.engawapg.app.camrepo.R
import net.engawapg.app.camrepo.databinding.*
import net.engawapg.app.camrepo.util.EventObserver
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
        pageItemAdapter = PageItemAdapter()

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
                viewModel.reload()
                pageItemAdapter.notifyDataSetChanged()
                binding.recyclerView.scrollToPosition(viewModel.getItemCount() - 2)
                viewModel.modified = true
            }
        })

        return binding.root
    }

    override fun onDestroyView() {
        closeKeyboard()
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.title = ""

        /* ViewModelからのUIイベント受信 */
        viewModel.uiEvent.observe(viewLifecycleOwner, EventObserver { event ->
            when (event) {
                PageViewModel.UI_EVENT_ON_CLICK_TAKE_PICTURE -> {
                    showCameraFragment()
                }
                PageViewModel.UI_EVENT_ON_CLICK_ADD_PICTURE -> {
                }
                PageViewModel.UI_EVENT_ON_FOCUS_CHANGE_TO_TEXT_EDIT -> {
                    hideCameraFragment()
                }
            }
        })
        viewModel.photoClickEvent.observe(viewLifecycleOwner, EventObserver { index ->
            closeKeyboard()
            val action = PageFragmentDirections.actionPageFragmentToPhotoPagerFragment(
                pageIndex = viewModel.pageIndex, photoIndex = index
            )
            findNavController().navigate(action)
        })
    }

    override fun onPause() {
        viewModel.save()
        super.onPause()
    }

    private fun showCameraFragment() {
        var cf = childFragmentManager.findFragmentById(cameraFragmentId)
        if (cf == null) {
            closeKeyboard()
            /* EditTextからフォーカスを外す */
            binding.root.requestFocus()

            cf = CameraFragment.newInstance()
            childFragmentManager.beginTransaction()
                .add(R.id.cameraFragmentContainer, cf)
                .runOnCommit {
                    /* カメラアイコンが見えるようにスクロール */
                    binding.recyclerView.scrollToPosition(viewModel.getItemCount() - 2)
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

    private fun closeKeyboard() {
        val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(activity?.currentFocus?.windowToken,
            InputMethodManager.HIDE_NOT_ALWAYS)
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
            else -> super.onOptionsItemSelected(item)
        }
    }

    private val actionModeCallback = object: ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            mode?.menuInflater?.inflate(R.menu.menu_page_action_mode, menu)
            viewModel.setEditMode(true)
            pageItemAdapter.notifyDataSetChanged()
            itemTouchHelper.attachToRecyclerView(null)
            closeKeyboard()
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
            viewModel.setEditMode(false)
            pageItemAdapter.notifyDataSetChanged()
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

    inner class PageItemAdapter: RecyclerView.Adapter<PageItemViewHolder>() {

        override fun getItemCount() = viewModel.getItemCount()

        override fun getItemViewType(position: Int) = viewModel.getViewType(position)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageItemViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val binding = when (viewType) {
                PageViewModel.VIEW_TYPE_PAGE_TITLE -> ViewPageTitleBinding.inflate(inflater, parent, false)
                PageViewModel.VIEW_TYPE_PHOTO -> ViewPagePhotoBinding.inflate(inflater, parent, false)
                PageViewModel.VIEW_TYPE_ADD_PHOTO -> ViewPageAddPhotoBinding.inflate(inflater, parent, false)
                PageViewModel.VIEW_TYPE_MEMO -> ViewPageMemoBinding.inflate(inflater, parent, false)
                else -> ViewPageBlankBinding.inflate(inflater, parent, false)
            }
            return PageItemViewHolder(binding)
        }

        override fun onBindViewHolder(holder: PageItemViewHolder, position: Int) {
            when (holder.binding) {
                is ViewPagePhotoBinding -> {
                    holder.binding.item = viewModel.getPhotoItem(position)
                    val resolver = holder.itemView.context.contentResolver
                    val bmp = viewModel.getPhotoBitmap(position, resolver)
                    if (bmp != null) {
                        holder.binding.imageView.setImageBitmap(bmp)
                    } else {
                        holder.binding.imageView.setImageResource(R.drawable.imagenotfound)
                    }
                }
            }
            holder.binding.setVariable(BR.viewModel, viewModel)
            holder.binding.executePendingBindings()
        }
    }

    class PageItemViewHolder(val binding: ViewDataBinding): RecyclerView.ViewHolder(binding.root)

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
            if (viewHolder.itemViewType == PageViewModel.VIEW_TYPE_PHOTO) {
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
            return (target.itemViewType == PageViewModel.VIEW_TYPE_PHOTO)
        }

        override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
            super.onSelectedChanged(viewHolder, actionState)
            Log.d("PageFragment", "ItemTouchHelper onSelectedChanged")
            closeKeyboard()
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