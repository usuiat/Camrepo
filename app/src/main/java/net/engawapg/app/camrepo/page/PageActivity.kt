package net.engawapg.app.camrepo.page

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.Size
import android.view.*
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_page.*
import kotlinx.android.synthetic.main.view_page_memo.view.*
import kotlinx.android.synthetic.main.view_page_photo.view.*
import kotlinx.android.synthetic.main.view_page_title.view.*
import net.engawapg.app.camrepo.DeleteConfirmDialog
import net.engawapg.app.camrepo.R
import org.koin.android.viewmodel.ext.android.getViewModel
import org.koin.android.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class PageActivity : AppCompatActivity(), DeleteConfirmDialog.EventListener {
    private lateinit var viewModel: PageViewModel
    private val cameraViewModel: CameraViewModel by viewModel()
    private var actionMode: ActionMode? = null
    private lateinit var pageItemAdapter: PageItemAdapter
    private var cameraFragmentId = 0
    private var pageIndex = 0
    private lateinit var inputMethodManager: InputMethodManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_page)

        /* Get PageIndex */
        pageIndex = intent.getIntExtra(KEY_PAGE_INDEX, 0)
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
        itemTouchHelper.attachToRecyclerView(recyclerView)

        /* 写真追加イベントの監視 */
        cameraViewModel.eventAddImagePageIndex.observe(this, Observer { index ->
            if (index == pageIndex) {
                pageItemAdapter.notifyDataSetChanged()
                viewModel.modified = true
            }
        })

        /* 写真操作時にキーボードを閉じるためのやつ */
        inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
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
            cameraViewModel.currentPageIndex = pageIndex
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
            pageItemAdapter.setEditMode(true)
            itemTouchHelper.attachToRecyclerView(null)
            /* キーボード消す */
            inputMethodManager.hideSoftInputFromWindow(currentFocus?.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
            return true
        }

        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
            return true
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            pageItemAdapter.setEditMode(false)
            itemTouchHelper.attachToRecyclerView(recyclerView)
            /* PageTitleにフォーカスが当たってしまうのを防ぐ */
            rootLayout.requestFocus()
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

        override fun getItemCount() = viewModel.getItemCount(editMode)

        override fun getItemViewType(position: Int) = viewModel.getViewType(position, editMode)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
            return when (viewType) {
                PageViewModel.VIEW_TYPE_PAGE_TITLE -> PageTitleViewHolder.create(parent, viewModel)
                PageViewModel.VIEW_TYPE_PHOTO -> PhotoViewHolder.create(parent, viewModel)
                PageViewModel.VIEW_TYPE_ADD_PHOTO -> AddPhotoViewHolder.create(parent)
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
            itemView.pageTitle.apply {
                setText(viewModel.getPageTitle())
                isEnabled = !editMode
                addTextChangedListener(object: TextWatcher {
                    override fun afterTextChanged(s: Editable?) {
                        viewModel.setPageTitle(s.toString())
                    }
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int ) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                })
            }
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
            val photoIndex = position - if (editMode) 0 else 1
            val imageInfo = viewModel.getPhotoAt(photoIndex) ?: return
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

    class AddPhotoViewHolder(v: View): BaseViewHolder(v) {

        companion object {
            fun create(parent: ViewGroup): AddPhotoViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val view = layoutInflater.inflate(R.layout.view_page_add_photo, parent, false)
                return AddPhotoViewHolder(view)
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
            itemView.memo.apply {
                setText(viewModel.getMemo())
                isEnabled = !editMode
                addTextChangedListener(object: TextWatcher {
                    override fun afterTextChanged(s: Editable?) {
                        viewModel.setMemo(s.toString())
                    }
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int ) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                })
            }
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

    private val itemTouchHelper = ItemTouchHelper(object: ItemTouchHelper.SimpleCallback(
        0, 0
    ) {
        override fun getMovementFlags(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder
        ): Int {
            if (viewHolder is PhotoViewHolder) {
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
            return (target is PhotoViewHolder)
        }

        override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
            super.onSelectedChanged(viewHolder, actionState)
            Log.d(TAG, "ItemTouchHelper onSelectedChanged")
            inputMethodManager.hideSoftInputFromWindow(currentFocus?.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
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
        private const val IMAGE_SPAN_COUNT = 4
        const val KEY_PAGE_INDEX = "KeyPageIndex"

        private const val TAG = "PageActivity"
    }
}