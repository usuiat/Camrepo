package net.engawapg.app.camrepo.note

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import net.engawapg.app.camrepo.DeleteConfirmDialog
import net.engawapg.app.camrepo.R
import net.engawapg.app.camrepo.databinding.*
import net.engawapg.app.camrepo.notelist.EditTitleDialog
import net.engawapg.app.camrepo.page.PageActivity
import net.engawapg.app.camrepo.photo.PhotoActivity
import net.engawapg.app.camrepo.slideshow.SlideshowActivity
import org.koin.android.viewmodel.ext.android.viewModel

class NoteActivity : AppCompatActivity(), DeleteConfirmDialog.EventListener,
    EditTitleDialog.EventListener {

    private lateinit var binding: ActivityNoteBinding
    private val viewModel: NoteViewModel by viewModel()
    private var actionMode: ActionMode? = null
    private  lateinit var noteItemAdapter: NoteItemAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNoteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        /* ViewModelに写真の列数を設定し、recyclerView表示用リストを作成する。 */
        viewModel.initItemList(IMAGE_SPAN_COUNT)

        /* ToolBar */
        setSupportActionBar(binding.toolbar)
        supportActionBar?.let {
            it.setDisplayHomeAsUpEnabled(true)
            it.setHomeButtonEnabled(true)
            it.title = ""
        }

        /* RecyclerView */
        noteItemAdapter = NoteItemAdapter(viewModel, itemTouchHelper) {
            onItemClick(it)
        }
        binding.recyclerView.apply {
            layoutManager = GridLayoutManager(context, IMAGE_SPAN_COUNT).apply {
                spanSizeLookup = NoteItemSpanSizeLookup()
            }
            adapter = noteItemAdapter
        }

        itemTouchHelper.attachToRecyclerView(binding.recyclerView)

        binding.floatingActionButton.setOnClickListener {
            onClickAddButton()
        }
    }

    override fun onResume() {
        super.onResume()
        if (viewModel.isPageAdded()) {
            noteItemAdapter.notifyDataSetChanged()
            binding.recyclerView.scrollToPosition(noteItemAdapter.itemCount - 1)
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
                val dialog = EditTitleDialog()
                dialog.arguments = Bundle().apply {
                    putInt(EditTitleDialog.KEY_TITLE, R.string.edit_note_title)
                    putString(EditTitleDialog.KEY_NOTE_TITLE, viewModel.getNoteTitle())
                    putString(EditTitleDialog.KEY_NOTE_SUB_TITLE, viewModel.getNoteSubTitle())
                }
                dialog.show(supportFragmentManager, EDIT_TITLE_DIALOG)
            }
            NoteViewModel.VIEW_TYPE_PAGE_TITLE, NoteViewModel.VIEW_TYPE_MEMO,
            NoteViewModel.VIEW_TYPE_BLANK -> {
                startActivity(Intent(this, PageActivity::class.java).apply {
                    putExtra(PageActivity.KEY_PAGE_INDEX, viewModel.getPageIndex(position))
                })
            }
            NoteViewModel.VIEW_TYPE_PHOTO -> {
                startActivity(Intent(this, PhotoActivity::class.java).apply {
                    putExtra(PhotoActivity.KEY_PAGE_INDEX, viewModel.getPageIndex(position))
                    putExtra(PhotoActivity.KEY_PHOTO_INDEX, viewModel.getPhotoIndex(position))
                    putExtra(PhotoActivity.KEY_WHOLE_OF_NOTE, true)
                })
            }
        }
    }

    override fun onClickOkAtEditTitleDialog(title: String, subTitle: String) {
        viewModel.setNoteTitle(title, subTitle)
        noteItemAdapter.notifyItemChanged(0)
    }

    private fun onClickAddButton() {
        viewModel.addPage()
        val newPageIndex = viewModel.getPageIndex(noteItemAdapter.itemCount - 1)
        Log.d(TAG, "Page added. itemCount = ${noteItemAdapter.itemCount}, pageIndex = $newPageIndex")
        startActivity(Intent(this, PageActivity::class.java).apply {
            putExtra(PageActivity.KEY_PAGE_INDEX, newPageIndex)
        })
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_note, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.edit_list_items -> {
                actionMode = startActionMode(actionModeCallback)
                true
            }
            R.id.slideshow -> {
                startActivity(Intent(this, SlideshowActivity::class.java).apply {
                    putExtra(SlideshowActivity.KEY_PAGE_INDEX, 0)
                })
                true
            }
            android.R.id.home -> {
                finish()
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
            binding.floatingActionButton.visibility = View.INVISIBLE
            return true
        }

        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
            if (item?.itemId == R.id.delete_selected_items) {
                if (viewModel.isPageSelected()) {
                    DeleteConfirmDialog().show(supportFragmentManager, DELETE_CONFIRM_DIALOG)
                }
            }
            return true
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            viewModel.setPageTitleListMode(false)
            noteItemAdapter.setEditMode(false)
            binding.floatingActionButton.visibility = View.VISIBLE
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?) = false
    }

    override fun onClickDeleteButton() {
        viewModel.deleteSelectedPages()
        actionMode?.finish()
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

    class TitleViewHolder(private val binding: ViewNoteTitleBinding,
                          private val viewModel: NoteViewModel) :BaseViewHolder(binding.root) {

        companion object {
            fun create(parent: ViewGroup, viewModel: NoteViewModel): TitleViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ViewNoteTitleBinding.inflate(layoutInflater, parent, false)
                return TitleViewHolder(binding, viewModel)
            }
        }

        override fun bind(position: Int, editMode: Boolean) {
            binding.title.text = viewModel.getNoteTitle()
            binding.subtitle.text = viewModel.getNoteSubTitle()
        }
    }

    class PageTitleViewHolder(private val binding: ViewNotePageTitleBinding,
                              private val viewModel: NoteViewModel,
                              private val itemTouchHelper: ItemTouchHelper)
        :BaseViewHolder(binding.root) {

        companion object {
            fun create(parent: ViewGroup, viewModel: NoteViewModel,
                       itemTouchHelper: ItemTouchHelper): PageTitleViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ViewNotePageTitleBinding.inflate(layoutInflater, parent, false)
                return PageTitleViewHolder(binding, viewModel, itemTouchHelper)
            }
        }

        override fun bind(position: Int, editMode: Boolean) {
            binding.pageTitle.text = viewModel.getPageTitle(position)
            binding.editButton.apply {
                visibility = if (editMode) View.GONE else View.VISIBLE
            }
            binding.pageCheckBox.apply {
                visibility = if (editMode) View.VISIBLE else View.GONE
                if (editMode) {
                    isChecked = viewModel.getPageSelection(position)
                    setOnClickListener {
                        viewModel.setPageSelection(adapterPosition, isChecked)
                    }
                }
            }
            binding.dragHandle.apply {
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

    class PhotoViewHolder(private val binding: ViewNotePhotoBinding,
                          private val viewModel: NoteViewModel) :BaseViewHolder(binding.root) {

        companion object {
            fun create(parent: ViewGroup, viewModel: NoteViewModel): PhotoViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ViewNotePhotoBinding.inflate(layoutInflater, parent, false)
                return PhotoViewHolder(binding, viewModel)
            }
        }

        override fun bind(position: Int, editMode: Boolean) {
            val resolver = itemView.context.contentResolver
            val bmp = viewModel.getPhotoBitmap(position, resolver)

            if (bmp != null) {
                binding.imageView.setImageBitmap(bmp)
            } else {
                binding.imageView.setImageResource(R.drawable.imagenotfound)
                Log.d(TAG, "Image is not exist @position = $position.")
            }
        }
    }

    class MemoViewHolder(private val binding: ViewNoteMemoBinding,
                         private val viewModel: NoteViewModel) :BaseViewHolder(binding.root) {

        companion object {
            fun create(parent: ViewGroup, viewModel: NoteViewModel): MemoViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ViewNoteMemoBinding.inflate(layoutInflater, parent, false)
                return MemoViewHolder(binding, viewModel)
            }
        }

        override fun bind(position: Int, editMode: Boolean) {
            binding.memo.text = viewModel.getMemo(position)
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
        private const val TAG = "NoteActivity"

        private const val EDIT_TITLE_DIALOG = "EditTitleDialog"
        private const val IMAGE_SPAN_COUNT = 4
        private const val DELETE_CONFIRM_DIALOG = "DeleteConfirmDialog"
    }
}