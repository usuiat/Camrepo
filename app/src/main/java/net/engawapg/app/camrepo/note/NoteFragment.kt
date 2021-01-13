package net.engawapg.app.camrepo.note

import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_note.*
import kotlinx.android.synthetic.main.view_note_page_title.view.*
import net.engawapg.app.camrepo.DeleteConfirmDialog
import net.engawapg.app.camrepo.R
import net.engawapg.app.camrepo.databinding.*
import net.engawapg.app.camrepo.notelist.EditTitleDialog
import net.engawapg.app.camrepo.util.EventObserver
import org.koin.android.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class NoteFragment : Fragment() {

    private val args: NoteFragmentArgs by navArgs()
    private val viewModel: NoteViewModel by viewModel{ parametersOf(args.fileName) } // Fragmentに紐づけ
    private var actionMode: ActionMode? = null
    private lateinit var noteItemAdapter: NoteItemAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = DataBindingUtil.inflate<FragmentNoteBinding>(
            inflater, R.layout.fragment_note, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        setHasOptionsMenu(true) /* Toolbarにメニューあり */
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        /* RecyclerView */
        noteItemAdapter = NoteItemAdapter(viewModel, viewLifecycleOwner, itemTouchHelper)
        recyclerView.apply {
            layoutManager = GridLayoutManager(context, IMAGE_SPAN_COUNT).apply {
                spanSizeLookup = NoteItemSpanSizeLookup()
            }
            adapter = noteItemAdapter
        }

        itemTouchHelper.attachToRecyclerView(recyclerView)

//        viewModel.pageModified.observe(viewLifecycleOwner, Observer {
//            if (viewModel.pageModified.value == true) {
//                viewModel.buildItemList()
//                noteItemAdapter.notifyDataSetChanged()
//                Log.d(TAG, "pageModified")
//                viewModel.pageModified.value = false
//            }
//        })

        viewModel.onClickTitle.observe(viewLifecycleOwner, EventObserver {
            findNavController().navigate(R.id.action_noteFragment_to_editTitleDialog)
        })
        viewModel.onSelectPage.observe(viewLifecycleOwner, EventObserver { pageIndex ->
            findNavController().navigate(
                NoteFragmentDirections.actionNoteFragmentToPageFragment(pageIndex))
        })
        viewModel.onSelectPhoto.observe(viewLifecycleOwner, EventObserver { photoIndex ->
            findNavController().navigate(
                NoteFragmentDirections.actionNoteFragmentToPhotoPagerFragment(
                    photoIndex.pageIndex, photoIndex.photoIndex, true
                ))
        })

        findNavController().currentBackStackEntry?.savedStateHandle
            ?.getLiveData<Int>(DeleteConfirmDialog.KEY_RESULT)
            ?.observe(viewLifecycleOwner) { result ->
                onDeleteConfirmDialogResult(result)
            }
        findNavController().currentBackStackEntry?.savedStateHandle
            ?.getLiveData<Int>(EditTitleDialog.KEY_RESULT)
            ?.observe(viewLifecycleOwner) {result ->
                Log.d(TAG, "EditTitleDialog result = $result")
                viewModel.buildItemList()
                noteItemAdapter.notifyDataSetChanged()
            }
    }

    override fun onResume() {
        super.onResume()
//        if (viewModel.isPageAdded()) {
//            noteItemAdapter.notifyDataSetChanged()
//            recyclerView.scrollToPosition(noteItemAdapter.itemCount - 1)
//        }
//        if (viewModel.isModifiedAfterLastDisplayedTime()) {
//            Log.d(TAG, "Note Updated")
//            viewModel.buildItemList()
//            noteItemAdapter.notifyDataSetChanged()
//        }
    }

    override fun onPause() {
        viewModel.save()
        super.onPause()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_note, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.edit_list_items -> {
                actionMode = activity?.startActionMode(actionModeCallback)
                true
            }
            R.id.slideshow -> {
                findNavController().navigate(R.id.action_noteFragment_to_slideshowActivity)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private val actionModeCallback = object: ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            mode?.menuInflater?.inflate(R.menu.menu_note_action_mode, menu)
            viewModel.setEditMode(true)
            noteItemAdapter.notifyDataSetChanged()
            return true
        }

        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
            if (item?.itemId == R.id.delete_selected_items) {
                if (viewModel.isPageSelected()) {
                    findNavController().navigate(R.id.action_global_deleteConfirmDialog)
                }
            }
            return true
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            viewModel.setEditMode(false)
            noteItemAdapter.notifyDataSetChanged()
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?) = false
    }

    private fun onDeleteConfirmDialogResult(result: Int) {
        if (result == DeleteConfirmDialog.RESULT_DELETE) {
            viewModel.deleteSelectedPages()
            actionMode?.finish()
        }
    }

    class NoteItemAdapter(private val viewModel: NoteViewModel,
                          private val lifecycleOwner: LifecycleOwner,
                          private val itemTouchHelper: ItemTouchHelper)
        : RecyclerView.Adapter<NoteItemViewHolder>() {

        override fun getItemCount() = viewModel.getItemCount()

        override fun getItemViewType(position: Int) = viewModel.getViewType(position)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteItemViewHolder {
            val layoutInflater = LayoutInflater.from(parent.context)
            val binding = when(viewType) {
                NoteViewModel.VIEW_TYPE_TITLE ->
                    DataBindingUtil.inflate<ViewNoteTitleBinding>(
                        layoutInflater, R.layout.view_note_title, parent, false)
                NoteViewModel.VIEW_TYPE_PAGE_TITLE ->
                    DataBindingUtil.inflate<ViewNotePageTitleBinding>(
                        layoutInflater, R.layout.view_note_page_title, parent, false)
                NoteViewModel.VIEW_TYPE_PHOTO ->
                    DataBindingUtil.inflate<ViewNotePhotoBinding>(
                        layoutInflater, R.layout.view_note_photo, parent, false)
                NoteViewModel.VIEW_TYPE_MEMO ->
                    DataBindingUtil.inflate<ViewNoteMemoBinding>(
                        layoutInflater, R.layout.view_note_memo, parent, false)
                else ->
                    DataBindingUtil.inflate<ViewNoteBlankBinding>(
                        layoutInflater, R.layout.view_note_blank, parent, false)
            }
            binding.lifecycleOwner = lifecycleOwner
            val holder = NoteItemViewHolder(binding)
            setupItemTouchHelper(viewType, holder)
            return holder
        }

        private fun setupItemTouchHelper(viewType: Int, holder: NoteItemViewHolder) {
            if (viewType == NoteViewModel.VIEW_TYPE_PAGE_TITLE) {
                holder.itemView.dragHandle.setOnTouchListener { v, event ->
                    if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                        itemTouchHelper.startDrag(holder)
                    } else {
                        v.performClick()
                    }
                    true
                }
            }
        }

        override fun onBindViewHolder(holder: NoteItemViewHolder, position: Int) {
            when (holder.binding) {
                is ViewNoteTitleBinding -> {
                    holder.binding.viewModel = viewModel
                    holder.binding.item = viewModel.getItem(position) as NoteTitleItem
                }
                is ViewNotePageTitleBinding -> {
                    holder.binding.viewModel = viewModel
                    holder.binding.item = viewModel.getItem(position) as NotePageTitleItem
                }
                is ViewNotePhotoBinding -> {
                    holder.binding.viewModel = viewModel
                    holder.binding.item = viewModel.getItem(position) as NotePhotoItem
                }
                is ViewNoteMemoBinding -> {
                    holder.binding.viewModel = viewModel
                    holder.binding.item = viewModel.getItem(position) as NoteMemoItem
                }
                is ViewNoteBlankBinding -> {
                    holder.binding.viewModel = viewModel
                    holder.binding.item = viewModel.getItem(position) as NoteBlankItem
                }
            }
            holder.binding.executePendingBindings()
        }
    }

    class NoteItemViewHolder(val binding: ViewDataBinding): RecyclerView.ViewHolder(binding.root)

//    class PhotoViewHolder(v: View, private val viewModel: NoteViewModel) :BaseViewHolder(v) {
//
//        override fun bind(position: Int, editMode: Boolean) {
//            val resolver = itemView.context.contentResolver
//            val bmp = viewModel.getPhotoBitmap(position, resolver)
//
//            if (bmp != null) {
//                itemView.imageView.setImageBitmap(bmp)
//            } else {
//                itemView.imageView.setImageResource(R.drawable.imagenotfound)
//                Log.d(TAG, "Image is not exist @position = $position.")
//            }
//        }
//    }

    inner class NoteItemSpanSizeLookup: GridLayoutManager.SpanSizeLookup() {
        override fun getSpanSize(position: Int): Int {
            return when (noteItemAdapter.getItemViewType(position)) {
                NoteViewModel.VIEW_TYPE_TITLE -> IMAGE_SPAN_COUNT
                NoteViewModel.VIEW_TYPE_PAGE_TITLE -> IMAGE_SPAN_COUNT
                NoteViewModel.VIEW_TYPE_MEMO -> IMAGE_SPAN_COUNT
                else -> 1
            }
        }
    }

    private val itemTouchHelper = ItemTouchHelper(object: ItemTouchHelper.SimpleCallback(
        (ItemTouchHelper.UP or ItemTouchHelper.DOWN), 0) {

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            val fromPosition = viewHolder.adapterPosition
            val toPosition = target.adapterPosition
            viewModel.movePage(fromPosition, toPosition)
            noteItemAdapter.notifyItemMoved(fromPosition, toPosition)
            return true
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit
        override fun isLongPressDragEnabled() = false
    })

    companion object {
        private const val TAG = "NoteFragment"
        private const val IMAGE_SPAN_COUNT = 4
//        @JvmStatic
//        fun newInstance() = NoteFragment()
    }
}