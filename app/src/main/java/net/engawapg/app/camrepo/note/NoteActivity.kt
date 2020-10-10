package net.engawapg.app.camrepo.note

import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_note.*
import kotlinx.android.synthetic.main.view_note_title.view.*
import net.engawapg.app.camrepo.DeleteConfirmDialog
import net.engawapg.app.camrepo.R
import org.koin.android.viewmodel.ext.android.viewModel

class NoteActivity : AppCompatActivity(), DeleteConfirmDialog.EventListener {
    private val viewModel: NoteViewModel by viewModel()
    private var actionMode: ActionMode? = null
    private  lateinit var pageCardAdapter: PageCardAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note)

        val noteIndex = intent.getIntExtra(INTENT_KEY_NOTE_INDEX, -1)
        if (noteIndex >= 0) {
            viewModel.setNoteIndex(noteIndex)
        }

        /* ToolBar */
        setSupportActionBar(toolbar)
        supportActionBar?.let {
            it.setDisplayHomeAsUpEnabled(true)
            it.setHomeButtonEnabled(true)
            it.title = ""
        }

        /* RecyclerView */
        pageCardAdapter = PageCardAdapter(viewModel)
        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = pageCardAdapter
        }

        floatingActionButton.setOnClickListener {

        }
    }

    override fun onPause() {
        super.onPause()
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
            else -> super.onOptionsItemSelected(item)
        }
    }

    private val actionModeCallback = object: ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            return true
        }

        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
            return true
        }

        override fun onDestroyActionMode(mode: ActionMode?) {

        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?) = false
    }

    override fun onClickDeleteButton() {
        actionMode?.finish()
    }

    class PageCardAdapter(private val viewModel: NoteViewModel)
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
                else -> TitleViewHolder.create(parent, viewModel)
            }
        }

        override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
            holder.bind(position, editMode)
        }
    }

    open class BaseViewHolder(v: View): RecyclerView.ViewHolder(v) {
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
            itemView.subtitle.text = viewModel.getNoteSubtitle()
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
        }
    }

    companion object {
//        private const val TAG = "NoteActivity"

        const val INTENT_KEY_NOTE_INDEX = "IntentKeyNoteIndex"
    }
}