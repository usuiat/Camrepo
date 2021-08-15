package net.engawapg.app.camrepo.page

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import androidx.constraintlayout.widget.ConstraintSet
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import net.engawapg.app.camrepo.BR
import net.engawapg.app.camrepo.R
import net.engawapg.app.camrepo.databinding.*
import net.engawapg.app.camrepo.model.ImageInfo
import net.engawapg.app.camrepo.util.EventObserver
import net.engawapg.app.camrepo.util.SimpleDialog
import org.koin.android.viewmodel.ViewModelOwner.Companion.from
import org.koin.android.viewmodel.ext.android.getViewModel
import org.koin.android.viewmodel.ext.android.sharedViewModel
import org.koin.core.parameter.parametersOf

class PageFragment: Fragment(), SimpleDialog.ResultListener {

    companion object {
        const val IMAGE_SPAN_COUNT = 4
        private const val DELETE_CONFIRM_DIALOG = "DeleteConfirmDialog"

        /* 表示中のフラグメント */
        private const val FRAGMENT_CAMERA = "FragmentCamera"
        private const val FRAGMENT_GALLERY = "FragmentGallery"
    }

    private val args: PageFragmentArgs by navArgs()
    private var _binding: FragmentPageBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: PageViewModel
    private val cameraViewModel: CameraViewModel by sharedViewModel(owner = { from(this)} )
    private val galleryViewModel: PhotoGalleryViewModel by sharedViewModel(owner = { from(this)})
    private var actionMode: ActionMode? = null
    private lateinit var pageItemAdapter: PageItemAdapter
    private var bottomFragmentTag: String? = null

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
        cameraViewModel.currentPageIndex = viewModel.pageIndex

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
                    switchBottomFragment(FRAGMENT_CAMERA)
                }
                PageViewModel.UI_EVENT_ON_CLICK_ADD_PICTURE -> {
                    switchBottomFragment(FRAGMENT_GALLERY)
                }
                PageViewModel.UI_EVENT_ON_FOCUS_CHANGE_TO_TEXT_EDIT -> {
                    switchBottomFragment(null)
                }
            }
        })
        viewModel.photoClickEvent.observe(viewLifecycleOwner, EventObserver { index ->
            deleteBottomFragment()
            closeKeyboard()
            val action = PageFragmentDirections.actionPageFragmentToPhotoPagerFragment(
                pageIndex = viewModel.pageIndex, photoIndex = index
            )
            findNavController().navigate(action)
        })
        cameraViewModel.uiEvent.observe(viewLifecycleOwner, EventObserver { event ->
            when (event) {
                CameraViewModel.UI_EVENT_ON_CLICK_CLOSE -> switchBottomFragment(null)
            }
        })
        galleryViewModel.onSelect.observe(viewLifecycleOwner, EventObserver {photoGalleryItem ->
            viewModel.addImageInfo(ImageInfo(photoGalleryItem.uri))
            pageItemAdapter.notifyDataSetChanged()
            binding.recyclerView.scrollToPosition(viewModel.getItemCount() - 2)
        })
        galleryViewModel.uiEvent.observe(viewLifecycleOwner, EventObserver { event ->
            when (event) {
                PhotoGalleryViewModel.UI_EVENT_ON_CLICK_CLOSE -> switchBottomFragment(null)
            }
        })
    }

    override fun onPause() {
        viewModel.save()
        super.onPause()
    }

    private fun deleteBottomFragment() {
        childFragmentManager.findFragmentByTag(bottomFragmentTag)?.let {
            childFragmentManager.beginTransaction().remove(it).commitNow()
        }
        bottomFragmentTag = null
    }

    private fun switchBottomFragment(tag: String?) {
        if ((tag == null) && (bottomFragmentTag == null)) {
            return /* 何もしない */
        }

        var newTag = tag
        if (newTag == bottomFragmentTag) {
            /* 表示中の画面と同じ画面を指定した場合は消す（トグル）*/
            newTag = null
        }

        if (bottomFragmentTag == null) {
            closeKeyboard()
            /* EditTextからフォーカスを外す */
            binding.root.requestFocus()
        }

        val oldFragment = childFragmentManager.findFragmentByTag(bottomFragmentTag)
        val newFragment = when (newTag) {
            FRAGMENT_CAMERA -> CameraFragment.newInstance()
            FRAGMENT_GALLERY -> PhotoGalleryFragment()
            else -> null
        }

        if ((oldFragment == null) && (newFragment != null)) {
            /* 下からスライドイン */
            slideInFromBottom(newFragment, newTag)
        } else if ((oldFragment != null) && (newFragment == null)) {
            /* 下にスライドアウト */
            slideOutToBottom(oldFragment)
        } else if ((oldFragment != null) && (newFragment != null)){
            /* 置き換え */
            childFragmentManager.beginTransaction()
                .replace(R.id.bottomFragmentContainer, newFragment, newTag)
                .commit()
        }

        bottomFragmentTag = if (newFragment != null) newTag else null
    }

    private fun slideInFromBottom(fragment: Fragment, tag: String?) {

        /* bottomFragmentContainerのBOTTOMをrootLayoutのBOTTOMに合わせる */
        ConstraintSet().apply {
            clone(binding.rootLayout)
            clear(R.id.bottomFragmentContainer, ConstraintSet.TOP)
            connect(R.id.bottomFragmentContainer, ConstraintSet.BOTTOM, R.id.rootLayout, ConstraintSet.BOTTOM)
            applyTo(binding.rootLayout)
        }

        /* フラグメントを表示 */
        childFragmentManager.beginTransaction()
            .add(R.id.bottomFragmentContainer, fragment, tag)
            .commit()

        /* スライドインアニメーション */
        ObjectAnimator.ofFloat(
            binding.bottomFragmentContainer, "translationY",
            binding.bottomFragmentContainer.measuredHeight.toFloat(), 0f
        ).apply {
            duration = resources.getInteger(R.integer.anim_time).toLong()
            start()
        }
    }

    private fun slideOutToBottom(fragment: Fragment) {

        /* bottomFragmentContainerのTOPをrootLayoutのBOTTOMに合わせる */
        ConstraintSet().apply {
            clone(binding.rootLayout)
            clear(R.id.bottomFragmentContainer, ConstraintSet.BOTTOM)
            connect(R.id.bottomFragmentContainer, ConstraintSet.TOP, R.id.rootLayout, ConstraintSet.BOTTOM)
            applyTo(binding.rootLayout)
        }

        /* アニメーションイベントリスナー。アニメーション終了したらフラグメントを削除 */
        val listener = object: AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                childFragmentManager.beginTransaction().remove(fragment).commit()
            }
        }

        /* スライドアウトアニメーション */
        ObjectAnimator.ofFloat(
            binding.bottomFragmentContainer, "translationY",
            -binding.bottomFragmentContainer.measuredHeight.toFloat(), 0f
        ).apply {
            duration = resources.getInteger(R.integer.anim_time).toLong()
            addListener(listener)
            start()
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
                switchBottomFragment(null)
                actionMode = activity?.startActionMode(actionModeCallback)
                true
            }
            R.id.slideshow -> {
                deleteBottomFragment()
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
                    SimpleDialog.Builder()
                        .setMessage(R.string.delete_confirm_message)
                        .setPositiveText(R.string.delete)
                        .setNegativeText(R.string.cancel)
                        .create()
                        .show(childFragmentManager, DELETE_CONFIRM_DIALOG)
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

    override fun onSimpleDialogResult(tag: String?, result: SimpleDialog.Result) {
        if (result == SimpleDialog.Result.POSITIVE) {
            viewModel.deleteSelectedPhotos()
            actionMode?.finish()
        }
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
                    val uri = viewModel.getPhotoItem(position)?.imageInfo?.uri
                    Picasso.get()
                        .load(uri)
                        .error(R.drawable.imagenotfound)
                        .fit()
                        .centerInside()
                        .into(holder.binding.imageView)
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