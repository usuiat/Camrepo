package net.engawapg.app.camrepo.page

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_page.*
import net.engawapg.app.camrepo.DeleteConfirmDialog
import net.engawapg.app.camrepo.R
import net.engawapg.app.camrepo.databinding.*
import net.engawapg.app.camrepo.util.EventObserver
import org.koin.android.viewmodel.ext.android.getViewModel
import org.koin.android.viewmodel.ext.android.sharedViewModel
import org.koin.core.parameter.parametersOf

class PageFragment : Fragment() {

    private val args: PageFragmentArgs by navArgs()
    private lateinit var viewModel: PageViewModel
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
        pageItemAdapter = PageItemAdapter(viewModel, viewLifecycleOwner)

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
                viewModel.reload()
                pageItemAdapter.notifyDataSetChanged()
                recyclerView.scrollToPosition(viewModel.getItemCount() - 2)
            }
        })

        /* 写真操作時にキーボードを閉じるためのやつ */
        inputMethodManager = activity?.getSystemService(Context.INPUT_METHOD_SERVICE)
                as InputMethodManager

        viewModel.onClickPhoto.observe(viewLifecycleOwner, EventObserver { photoIndex ->
            findNavController().navigate(
                PageFragmentDirections.actionPageFragmentToPhotoPagerFragment(
                    pageIndex, photoIndex, false))
        })
        viewModel.onClickAddPhoto.observe(viewLifecycleOwner, EventObserver {
            showCameraFragment()
        })
        viewModel.onEditTextFocused.observe(viewLifecycleOwner, EventObserver {
            hideCameraFragment()
        })

        findNavController().currentBackStackEntry?.savedStateHandle
            ?.getLiveData<Int>(DeleteConfirmDialog.KEY_RESULT)
            ?.observe(viewLifecycleOwner) { result ->
                onDeleteConfirmDialogResult(result)
            }
    }

    override fun onPause() {
        viewModel.save()
        /* キーボード消す */
        inputMethodManager.hideSoftInputFromWindow(activity?.currentFocus?.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
        super.onPause()
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
                    recyclerView.scrollToPosition(viewModel.getItemCount() - 2)
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
                val action = PageFragmentDirections.actionPageFragmentToSlideshowActivity(pageIndex)
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
            viewModel.setEditMode(false)
            pageItemAdapter.notifyDataSetChanged()
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
                          private val lifecycleOwner: LifecycleOwner)
        : RecyclerView.Adapter<PageItemViewHolder>() {

        override fun getItemCount() = viewModel.getItemCount()

        override fun getItemViewType(position: Int) = viewModel.getViewType(position)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageItemViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val binding = when(viewType) {
                PageViewModel.VIEW_TYPE_PAGE_TITLE -> ViewPageTitleBinding.inflate(inflater, parent, false)
                PageViewModel.VIEW_TYPE_PHOTO -> ViewPagePhotoBinding.inflate(inflater, parent, false)
                PageViewModel.VIEW_TYPE_ADD_PHOTO -> ViewPageAddPhotoBinding.inflate(inflater, parent, false)
                PageViewModel.VIEW_TYPE_MEMO -> ViewPageMemoBinding.inflate(inflater, parent, false)
                else -> ViewPageBlankBinding.inflate(inflater, parent, false)
            }
            binding.lifecycleOwner = lifecycleOwner
            return PageItemViewHolder(binding)
        }

        override fun onBindViewHolder(holder: PageItemViewHolder, position: Int) {
            when (holder.binding) {
                is ViewPageTitleBinding -> {
                    holder.binding.viewModel = viewModel
                }
                is ViewPagePhotoBinding -> {
                    holder.binding.viewModel = viewModel
                    viewModel.getPhotoItem(position)?.let {
                        it.loadPhoto(holder.itemView.context, R.drawable.imagenotfound)
                        holder.binding.item = it
                    }
                }
                is ViewPageAddPhotoBinding -> {
                    holder.binding.viewModel = viewModel
                }
                is ViewPageMemoBinding -> {
                    holder.binding.viewModel = viewModel
                }
            }
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
                return ItemTouchHelper.Callback.makeMovementFlags(drag, 0)
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