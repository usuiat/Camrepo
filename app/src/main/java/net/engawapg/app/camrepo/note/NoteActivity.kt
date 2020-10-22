package net.engawapg.app.camrepo.note

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_note.*
import kotlinx.android.synthetic.main.view_note_memo.view.*
import kotlinx.android.synthetic.main.view_note_page_title.view.*
import kotlinx.android.synthetic.main.view_note_title.view.*
import net.engawapg.app.camrepo.DeleteConfirmDialog
import net.engawapg.app.camrepo.R
import net.engawapg.app.camrepo.notelist.EditTitleDialog
import org.koin.android.viewmodel.ext.android.viewModel

class NoteActivity : AppCompatActivity(), DeleteConfirmDialog.EventListener,
    EditTitleDialog.EventListener {
    private val viewModel: NoteViewModel by viewModel()
    private var actionMode: ActionMode? = null
    private  lateinit var noteItemAdapter: NoteItemAdapter
    private var cameraFragmentId = 0

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

    override fun onPause() {
        viewModel.save()
        super.onPause()
    }

    private fun onItemClick(position: Int) {
        if (position == 0) {
            /* Note Title */
            val dialog = EditTitleDialog()
            dialog.arguments = Bundle().apply {
                putInt(EditTitleDialog.KEY_TITLE, R.string.edit_note_title)
                putString(EditTitleDialog.KEY_NOTE_TITLE, viewModel.getNoteTitle())
                putString(EditTitleDialog.KEY_NOTE_SUB_TITLE, viewModel.getNoteSubTitle())
            }
            dialog.show(supportFragmentManager, EDIT_TITLE_DIALOG)
        }
        else if(noteItemAdapter.getItemViewType(position) == NoteViewModel.VIEW_TYPE_ADD_PHOTO) {
            /* Add Photo */
            showCameraFragment()
        }
    }

    private fun showCameraFragment() {
        var cf = supportFragmentManager.findFragmentById(cameraFragmentId)
        if (cf == null) {
            cf = CameraFragment.newInstance()
            val trs = supportFragmentManager.beginTransaction()
            trs.add(R.id.cameraFragmentContainer, cf)
            trs.commit()
            cameraFragmentId = cf.id
        }
    }

    override fun onClickOkAtEditTitleDialog(title: String, subTitle: String) {
        viewModel.setNoteTitle(title, subTitle)
        noteItemAdapter.notifyItemChanged(0)
    }

    private fun onClickAddButton() {
        viewModel.addPage()
        /* リストの末尾に追加されるので、表示更新してスクロール */
        val lastPosition = viewModel.getItemCount() - 1
        noteItemAdapter.notifyItemInserted(lastPosition)
        recyclerView.scrollToPosition(lastPosition)
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
                NoteViewModel.VIEW_TYPE_ADD_PHOTO -> AddPhotoViewHolder.create(parent, viewModel)
                else -> BaseViewHolder.create(parent)
            }
        }

        override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
            holder.bind(position, editMode)
            holder.itemView.setOnClickListener {
                onItemClick(position)
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
            itemView.pageTitle.setText(viewModel.getPageTitle(position))
            itemView.pageTitle.addTextChangedListener(object: TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    viewModel.setPageTitle(position, s.toString())
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })
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

    class AddPhotoViewHolder(v: View, private val viewModel: NoteViewModel): BaseViewHolder(v) {

        companion object {
            fun create(parent: ViewGroup, viewModel: NoteViewModel): AddPhotoViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val view = layoutInflater.inflate(R.layout.view_note_add_photo, parent, false)
                return AddPhotoViewHolder(view, viewModel)
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
            itemView.memo.setText(viewModel.getMemo(position))
            itemView.memo.addTextChangedListener(object: TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    viewModel.setMemo(position, s.toString())
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })
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
//        private const val TAG = "NoteActivity"

        private const val EDIT_TITLE_DIALOG = "EditTitleDialog"
        private const val IMAGE_SPAN_COUNT = 4
    }
}