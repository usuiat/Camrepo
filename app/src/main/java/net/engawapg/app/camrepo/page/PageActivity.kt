package net.engawapg.app.camrepo.page

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_page.*
import kotlinx.android.synthetic.main.view_page_memo.view.*
import kotlinx.android.synthetic.main.view_page_title.view.*
import net.engawapg.app.camrepo.DeleteConfirmDialog
import net.engawapg.app.camrepo.R
import org.koin.android.viewmodel.ext.android.getViewModel
import org.koin.core.parameter.parametersOf

class PageActivity : AppCompatActivity(), DeleteConfirmDialog.EventListener {
    private lateinit var viewModel: PageViewModel
    private var actionMode: ActionMode? = null
    private lateinit var pageItemAdapter: PageItemAdapter
    private var cameraFragmentId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_page)

        /* Get PageIndex */
        val pageIndex = intent.getIntExtra(KEY_PAGE_INDEX, 0)
        viewModel = getViewModel { parametersOf(pageIndex, IMAGE_SPAN_COUNT) }

        /* ToolBar */
        setSupportActionBar(toolbar)
        supportActionBar?.let {
            it.setDisplayHomeAsUpEnabled(true)
            it.setHomeButtonEnabled(true)
            it.title = ""
        }

        /* RecyclerView */
        pageItemAdapter = PageItemAdapter(viewModel) { position ->
            onItemClick( position )
        }
        recyclerView.apply {
            layoutManager = GridLayoutManager(context, IMAGE_SPAN_COUNT).apply {
                spanSizeLookup = PageItemSpanSizeLookup()
            }
            adapter = pageItemAdapter
        }
    }

    override fun onPause() {
        viewModel.save()
        super.onPause()
    }

    private fun onItemClick(position: Int) {
        if (pageItemAdapter.getItemViewType(position) == PageViewModel.VIEW_TYPE_ADD_PHOTO) {
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


    class PageItemAdapter(private val viewModel: PageViewModel,
                          private val onItemClick: ((Int)->Unit)):
        RecyclerView.Adapter<BaseViewHolder>() {

        private var editMode = false
        fun setEditMode(mode: Boolean) {
            editMode = mode
            notifyDataSetChanged()
        }

        override fun getItemCount() = viewModel.getItemCount()

        override fun getItemViewType(position: Int) = viewModel.getViewType(position)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
            return when (viewType) {
                PageViewModel.VIEW_TYPE_PAGE_TITLE -> PageTitleViewHolder.create(parent, viewModel)
                PageViewModel.VIEW_TYPE_PHOTO -> PhotoViewHolder.create(parent, viewModel)
                PageViewModel.VIEW_TYPE_ADD_PHOTO -> AddPhotoViewHolder.create(parent, viewModel)
                PageViewModel.VIEW_TYPE_MEMO -> MemoViewHolder.create(parent, viewModel)
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

    class PageTitleViewHolder(v: View, private val viewModel: PageViewModel): BaseViewHolder(v) {

        companion object {
            fun create(parent: ViewGroup, viewModel: PageViewModel): PageTitleViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val view = layoutInflater.inflate(R.layout.view_page_title, parent, false)
                return PageTitleViewHolder(view, viewModel)
            }
        }

        override fun bind(position: Int, editMode: Boolean) {
            itemView.pageTitle.setText(viewModel.getPageTitle())
            itemView.pageTitle.addTextChangedListener(object: TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    viewModel.setPageTitle(s.toString())
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int ) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })
        }
    }

    class PhotoViewHolder(v: View, private val viewModel: PageViewModel): BaseViewHolder(v) {

        companion object {
            fun create(parent: ViewGroup, viewModel: PageViewModel): PhotoViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val view = layoutInflater.inflate(R.layout.view_page_photo, parent, false)
                return PhotoViewHolder(view, viewModel)
            }
        }

        override fun bind(position: Int, editMode: Boolean) {
        }
    }

    class AddPhotoViewHolder(v: View, private val viewModel: PageViewModel): BaseViewHolder(v) {

        companion object {
            fun create(parent: ViewGroup, viewModel: PageViewModel): AddPhotoViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val view = layoutInflater.inflate(R.layout.view_page_add_photo, parent, false)
                return AddPhotoViewHolder(view, viewModel)
            }
        }
    }

    class MemoViewHolder(v: View, private val viewModel: PageViewModel): BaseViewHolder(v) {

        companion object {
            fun create(parent: ViewGroup, viewModel: PageViewModel): MemoViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val view = layoutInflater.inflate(R.layout.view_page_memo, parent, false)
                return MemoViewHolder(view, viewModel)
            }
        }

        override fun bind(position: Int, editMode: Boolean) {
            itemView.memo.setText(viewModel.getMemo())
            itemView.memo.addTextChangedListener(object: TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    viewModel.setMemo(s.toString())
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int ) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })
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

    companion object {
        private const val IMAGE_SPAN_COUNT = 4
        const val KEY_PAGE_INDEX = "KeyPageIndex"
    }
}