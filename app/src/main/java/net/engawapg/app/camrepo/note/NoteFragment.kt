package net.engawapg.app.camrepo.note

import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_note.*
import kotlinx.android.synthetic.main.view_note_memo.view.*
import kotlinx.android.synthetic.main.view_note_page_title.view.*
import kotlinx.android.synthetic.main.view_note_photo.view.*
import kotlinx.android.synthetic.main.view_note_title.view.*
import net.engawapg.app.camrepo.DeleteConfirmDialog
import net.engawapg.app.camrepo.R
import net.engawapg.app.camrepo.notelist.EditTitleViewModel
import net.engawapg.app.camrepo.notelist.NoteListViewModel
import org.koin.android.viewmodel.ext.android.sharedViewModel

class NoteFragment : Fragment() {

    private val noteListViewModel: NoteListViewModel by sharedViewModel()
    private val viewModel: NoteViewModel by sharedViewModel()
    private val editTitleViewModel: EditTitleViewModel by sharedViewModel()
    private var actionMode: ActionMode? = null
    private lateinit var noteItemAdapter: NoteItemAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true) /* Toolbarにメニューあり */
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_note, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        /* RecyclerView */
        noteItemAdapter = NoteItemAdapter(viewModel, itemTouchHelper) {
            onItemClick(it)
        }
        recyclerView.apply {
            layoutManager = GridLayoutManager(context, IMAGE_SPAN_COUNT).apply {
                spanSizeLookup = NoteItemSpanSizeLookup()
            }
            adapter = noteItemAdapter
        }

        itemTouchHelper.attachToRecyclerView(recyclerView)

        floatingActionButton.setOnClickListener {
            onClickAddButton()
        }

        viewModel.noteProperty.observe(viewLifecycleOwner, Observer {
            noteItemAdapter.notifyDataSetChanged()
        })

        editTitleViewModel.onClickOk.observe(viewLifecycleOwner, Observer {
            if (editTitleViewModel.tag == TAG) {
                viewModel.setNoteTitle(editTitleViewModel.title, editTitleViewModel.subTitle)
                noteItemAdapter.notifyItemChanged(0)
//                noteListViewModel.updateCurrentNoteInfo()
            }
        })

        viewModel.pageModified.observe(viewLifecycleOwner, Observer {
            if (viewModel.pageModified.value == true) {
                viewModel.buildItemList()
                noteItemAdapter.notifyDataSetChanged()
                Log.d(TAG, "pageModified")
                viewModel.pageModified.value = false
            }
        })

        findNavController().currentBackStackEntry?.savedStateHandle
            ?.getLiveData<Int>(DeleteConfirmDialog.KEY_RESULT)
            ?.observe(viewLifecycleOwner) { result ->
                onDeleteConfirmDialogResult(result)
            }
    }

    override fun onResume() {
        super.onResume()
        if (viewModel.isPageAdded()) {
            noteItemAdapter.notifyDataSetChanged()
            recyclerView.scrollToPosition(noteItemAdapter.itemCount - 1)
        }
        if (viewModel.isModifiedAfterLastDisplayedTime()) {
            Log.d(TAG, "Note Updated")
            viewModel.buildItemList()
            noteItemAdapter.notifyDataSetChanged()
        }
    }

    override fun onPause() {
        viewModel.save()
        super.onPause()
    }

    private fun onItemClick(position: Int) {
        when (noteItemAdapter.getItemViewType(position)) {
            NoteViewModel.VIEW_TYPE_TITLE -> {
                editTitleViewModel.apply {
                    dialogTitle = getString(R.string.edit_note_title)
                    title = viewModel.getNoteTitle()
                    subTitle = viewModel.getNoteSubTitle()
                    tag = TAG
                }
                findNavController().navigate(R.id.action_noteFragment_to_editTitleDialog)
            }
            NoteViewModel.VIEW_TYPE_PAGE_TITLE, NoteViewModel.VIEW_TYPE_MEMO,
            NoteViewModel.VIEW_TYPE_BLANK -> {
                val pageIndex = viewModel.getPageIndex(position)
                val action = NoteFragmentDirections.actionNoteFragmentToPageFragment(pageIndex)
                findNavController().navigate(action)
            }
            NoteViewModel.VIEW_TYPE_PHOTO -> {
                val action = NoteFragmentDirections.actionNoteFragmentToPhotoPagerFragment(
                    viewModel.getPageIndex(position),
                    viewModel.getPhotoIndex(position),
                    true
                )
                findNavController().navigate(action)
            }
        }
    }

//    override fun onClickOkAtEditTitleDialog(title: String, subTitle: String) {
//        viewModel.setNoteTitle(title, subTitle)
//        noteItemAdapter.notifyItemChanged(0)
//    }

    private fun onClickAddButton() {
        viewModel.addPage()
        val newPageIndex = viewModel.getPageIndex(noteItemAdapter.itemCount - 1)
        Log.d(TAG, "Page added. itemCount = ${noteItemAdapter.itemCount}, pageIndex = $newPageIndex")
        val action = NoteFragmentDirections.actionNoteFragmentToPageFragment(newPageIndex)
        findNavController().navigate(action)
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
            viewModel.setPageTitleListMode(true)
            noteItemAdapter.setEditMode(true)
            floatingActionButton.visibility = View.INVISIBLE
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
            viewModel.setPageTitleListMode(false)
            noteItemAdapter.setEditMode(false)
            floatingActionButton.visibility = View.VISIBLE
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
                          private val itemTouchHelper: ItemTouchHelper,
                          private val onItemClick: ((Int)->Unit))
        : RecyclerView.Adapter<BaseViewHolder>() {

        private var editMode = false
        fun setEditMode(mode: Boolean) {
            editMode = mode
            notifyDataSetChanged()
        }

        override fun getItemCount() = viewModel.getItemCount()

        override fun getItemViewType(position: Int) = viewModel.getViewType(position)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
            return when (viewType) {
                NoteViewModel.VIEW_TYPE_PAGE_TITLE -> PageTitleViewHolder.create(parent, viewModel, itemTouchHelper)
                NoteViewModel.VIEW_TYPE_PHOTO -> PhotoViewHolder.create(parent, viewModel)
                NoteViewModel.VIEW_TYPE_MEMO -> MemoViewHolder.create(parent, viewModel)
                NoteViewModel.VIEW_TYPE_TITLE -> TitleViewHolder.create(parent, viewModel)
                else -> BaseViewHolder.create(parent)
            }
        }

        override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
            holder.bind(position, editMode)
            holder.itemView.setOnClickListener {
                if (!editMode) {
                    onItemClick(holder.adapterPosition)
                }
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

    class TitleViewHolder(v: View, private val viewModel: NoteViewModel) :BaseViewHolder(v) {

        companion object {
            fun create(parent: ViewGroup, viewModel: NoteViewModel): TitleViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val view = layoutInflater.inflate(R.layout.view_note_title, parent, false)
                return TitleViewHolder(view, viewModel)
            }
        }

        override fun bind(position: Int, editMode: Boolean) {
            itemView.title.text = viewModel.getNoteTitle()
            itemView.subtitle.text = viewModel.getNoteSubTitle()
        }
    }

    class PageTitleViewHolder(v: View, private val viewModel: NoteViewModel,
                              private val itemTouchHelper: ItemTouchHelper
    ) :BaseViewHolder(v) {

        companion object {
            fun create(parent: ViewGroup, viewModel: NoteViewModel,
                       itemTouchHelper: ItemTouchHelper
            ): PageTitleViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val view = layoutInflater.inflate(R.layout.view_note_page_title, parent, false)
                return PageTitleViewHolder(view, viewModel, itemTouchHelper)
            }
        }

        override fun bind(position: Int, editMode: Boolean) {
            itemView.pageTitle.text = viewModel.getPageTitle(position)
            itemView.editButton.apply {
                visibility = if (editMode) View.GONE else View.VISIBLE
            }
            itemView.pageCheckBox.apply {
                visibility = if (editMode) View.VISIBLE else View.GONE
                if (editMode) {
                    isChecked = viewModel.getPageSelection(position)
                    setOnClickListener {
                        viewModel.setPageSelection(adapterPosition, isChecked)
                    }
                }
            }
            itemView.dragHandle.apply {
                visibility = if (editMode) View.VISIBLE else View.GONE
                setOnTouchListener { v, event ->
                    if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                        itemTouchHelper.startDrag(this@PageTitleViewHolder)
                    } else {
                        v.performClick()
                    }
                    return@setOnTouchListener true
                }
            }
        }
    }

    class PhotoViewHolder(v: View, private val viewModel: NoteViewModel) :BaseViewHolder(v) {

        companion object {
            fun create(parent: ViewGroup, viewModel: NoteViewModel): PhotoViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val view = layoutInflater.inflate(R.layout.view_note_photo, parent, false)
                return PhotoViewHolder(view, viewModel)
            }
        }

        override fun bind(position: Int, editMode: Boolean) {
            val resolver = itemView.context.contentResolver
            val bmp = viewModel.getPhotoBitmap(position, resolver)

            if (bmp != null) {
                itemView.imageView.setImageBitmap(bmp)
            } else {
                itemView.imageView.setImageResource(R.drawable.imagenotfound)
                Log.d(TAG, "Image is not exist @position = $position.")
            }
        }
    }

    class MemoViewHolder(v: View, private val viewModel: NoteViewModel) :BaseViewHolder(v) {

        companion object {
            fun create(parent: ViewGroup, viewModel: NoteViewModel): MemoViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val view = layoutInflater.inflate(R.layout.view_note_memo, parent, false)
                return MemoViewHolder(view, viewModel)
            }
        }

        override fun bind(position: Int, editMode: Boolean) {
            itemView.memo.text = viewModel.getMemo(position)
        }
    }

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