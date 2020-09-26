package net.engawapg.app.camrepo.notelist

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_note_list.*
import kotlinx.android.synthetic.main.view_note_card.view.*
import net.engawapg.app.camrepo.DeleteConfirmDialog
import net.engawapg.app.camrepo.R
import net.engawapg.app.camrepo.note.NoteActivity
import org.koin.android.viewmodel.ext.android.viewModel

class NoteListActivity : AppCompatActivity(), DeleteConfirmDialog.EventListener {
    private val viewModel: NoteListViewModel by viewModel()
    private var actionMode: ActionMode? = null
    private lateinit var noteCardAdapter: NoteCardAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note_list)
        setSupportActionBar(toolbar)

        noteCardAdapter = NoteCardAdapter(viewModel)
        recyclerView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = noteCardAdapter
        }

        floatingActionButton.setOnClickListener {
            viewModel.onClickAdd()
            startActivityForResult(Intent(this, NoteActivity::class.java),
                RequestCode_NoteActivity)
        }
    }

    override fun onPause() {
        viewModel.save()
        super.onPause()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            RequestCode_NoteActivity -> {
                recyclerView.adapter?.notifyDataSetChanged()
                Log.d(TAG, "Return from NoteActivity")
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_note_list, menu)
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
            mode?.menuInflater?.inflate(R.menu.menu_note_list_action_mode, menu)
            viewModel.initSelection()
            noteCardAdapter.setEditMode(true)
            floatingActionButton.visibility = View.INVISIBLE
            return true
        }

        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
            if (item?.itemId == R.id.delete_selected_items) {
                if (viewModel.isSelected()) {
                    DeleteConfirmDialog().show(supportFragmentManager, DELETE_CONFIRM_DIALOG)
                }
            }
            return true
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            actionMode = null
            viewModel.clearSelection()
            noteCardAdapter.setEditMode(false)
            floatingActionButton.visibility = View.VISIBLE
            Log.d(TAG, "onDestroyActionMode")
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?) = false
    }

    override fun onClickDeleteButton() {
        Log.d(TAG, "onClickDeleteButton")
        viewModel.deleteSelectedItems()
        actionMode?.finish()
    }

    class NoteCardAdapter(private val viewModel: NoteListViewModel)
        : RecyclerView.Adapter<NoteCardViewHolder>() {

        private var editMode = false
        fun setEditMode(mode: Boolean) {
            editMode = mode
            notifyDataSetChanged()
        }

        override fun getItemCount(): Int {
            return viewModel.getItemCount()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteCardViewHolder {
            val layoutInflater = LayoutInflater.from(parent.context)
            val view = layoutInflater.inflate(R.layout.view_note_card, parent, false)
            return NoteCardViewHolder(view, viewModel)
        }

        override fun onBindViewHolder(holder: NoteCardViewHolder, position: Int) {
            holder.bind(position, editMode)
            Log.d(TAG, "onBindViewHolder at $position on $holder")
        }
    }

    class NoteCardViewHolder(v: View, private val viewModel: NoteListViewModel)
        : RecyclerView.ViewHolder(v) {

        fun bind(position: Int, editMode: Boolean) {
            itemView.title.text = viewModel.getTitle(position)
            itemView.subTitle.text = viewModel.getSubTitle(position)
            itemView.date.text = viewModel.getUpdateDate(position)
            itemView.cardView.setOnClickListener {
                if (!editMode) {
                    Log.d(TAG, "onClick Card at $position")
                    viewModel.selectNote(position)
                    it.context.startActivity(Intent(it.context, NoteActivity::class.java))
                }
            }

            /* CheckBox for delete operation */
            itemView.checkBox.visibility = if (editMode) View.VISIBLE else View.INVISIBLE
            itemView.checkBox.isChecked = viewModel.getSelection(position)
            itemView.checkBox.setOnClickListener {
                viewModel.setSelection(position, itemView.checkBox.isChecked)
            }
        }
    }

    companion object {
        private const val TAG = "NoteListActivity"
        private const val RequestCode_NoteActivity = 1
        private const val DELETE_CONFIRM_DIALOG = "DeleteConfirmDialog"
    }
}