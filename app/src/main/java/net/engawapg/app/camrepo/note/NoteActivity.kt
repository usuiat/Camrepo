package net.engawapg.app.camrepo.note

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_note.*
import kotlinx.android.synthetic.main.view_note_memo.view.*
import kotlinx.android.synthetic.main.view_note_page_title.view.*
import kotlinx.android.synthetic.main.view_note_title.view.*
import kotlinx.android.synthetic.main.view_note_photo.view.*
import net.engawapg.app.camrepo.DeleteConfirmDialog
import net.engawapg.app.camrepo.R
import net.engawapg.app.camrepo.notelist.EditTitleDialog
import net.engawapg.app.camrepo.page.PageActivity
import org.koin.android.viewmodel.ext.android.viewModel

class NoteActivity : AppCompatActivity(), DeleteConfirmDialog.EventListener,
    EditTitleDialog.EventListener {
    private val viewModel: NoteViewModel by viewModel()
    private var actionMode: ActionMode? = null
    private  lateinit var noteItemAdapter: NoteItemAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note)

        /* ViewModelに写真の列数を設定し、recyclerView表示用リストを作成する。 */
        viewModel.initItemList(IMAGE_SPAN_COUNT)

        /* ToolBar */
        setSupportActionBar(toolbar)
        supportActionBar?.let {
            it.setDisplayHomeAsUpEnabled(true)
            it.setHomeButtonEnabled(true)
            it.title = ""
        }

        /* RecyclerView */
        noteItemAdapter = NoteItemAdapter(viewModel) {
            onItemClick(it)
        }
        recyclerView.apply {
            layoutManager = GridLayoutManager(context, IMAGE_SPAN_COUNT).apply {
                spanSizeLookup = NoteItemSpanSizeLookup()
            }
            adapter = noteItemAdapter
        }

        floatingActionButton.setOnClickListener {
            onClickAddButton()
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
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?) = false
    }

    override fun onClickDeleteButton() {
        viewModel.deleteSelectedPages()
        actionMode?.finish()
    }

    class NoteItemAdapter(private val viewModel: NoteViewModel,
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
                NoteViewModel.VIEW_TYPE_PAGE_TITLE -> PageTitleViewHolder.create(parent, viewModel)
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

    class PageTitleViewHolder(v: View, private val viewModel: NoteViewModel) :BaseViewHolder(v) {

        companion object {
            fun create(parent: ViewGroup, viewModel: NoteViewModel): PageTitleViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val view = layoutInflater.inflate(R.layout.view_note_page_title, parent, false)
                return PageTitleViewHolder(view, viewModel)
            }
        }

        override fun bind(position: Int, editMode: Boolean) {
            itemView.pageTitle.text = viewModel.getPageTitle(position)
            itemView.pageCheckBox.apply {
                visibility = if (editMode) View.VISIBLE else View.GONE
                if (editMode) {
                    isChecked = viewModel.getPageSelection(position)
                    setOnClickListener {
                        viewModel.setPageSelection(adapterPosition, isChecked)
                    }
                }
            }
            itemView.dragHandle.visibility = if (editMode) View.VISIBLE else View.GONE
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
            val imageInfo = viewModel.getPhoto(position) ?: return
            val resolver = itemView.context.contentResolver

            val bmp = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                val id = imageInfo.uri.lastPathSegment?.toLong() ?: 0
                MediaStore.Images.Thumbnails.getThumbnail(
                    resolver, id,
                    MediaStore.Images.Thumbnails.MINI_KIND, null
                )
            } else {
                resolver?.loadThumbnail(imageInfo.uri, Size(256, 256), null)
            }

            itemView.imageView.setImageBitmap(bmp)
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

    companion object {
        private const val TAG = "NoteActivity"

        private const val EDIT_TITLE_DIALOG = "EditTitleDialog"
        private const val IMAGE_SPAN_COUNT = 4
        private const val DELETE_CONFIRM_DIALOG = "DeleteConfirmDialog"
    }
}