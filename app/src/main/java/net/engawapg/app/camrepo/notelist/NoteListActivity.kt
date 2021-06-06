package net.engawapg.app.camrepo.notelist

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.engawapg.app.camrepo.DeleteConfirmDialog
import net.engawapg.app.camrepo.R
import net.engawapg.app.camrepo.databinding.ActivityNoteListBinding
import net.engawapg.app.camrepo.databinding.ViewNoteCardBinding
import net.engawapg.app.camrepo.note.NoteActivity
import net.engawapg.app.camrepo.settings.SettingsActivity
import org.koin.android.viewmodel.ext.android.viewModel

class NoteListActivity : AppCompatActivity(), DeleteConfirmDialog.EventListener
    ,EditTitleDialog.EventListener {

    private lateinit var binding: ActivityNoteListBinding
    private val viewModel: NoteListViewModel by viewModel()
    private var actionMode: ActionMode? = null
    private lateinit var noteCardAdapter: NoteCardAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNoteListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = ""

        noteCardAdapter = NoteCardAdapter(viewModel)
        binding.recyclerView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = noteCardAdapter
        }

        binding.floatingActionButton.setOnClickListener { onClickAddButton() }
        Log.d(TAG, "onCreate")
    }

    override fun onResume() {
        super.onResume()
        if (viewModel.isCurrentNoteModified()) {
            noteCardAdapter.notifyDataSetChanged()
            binding.recyclerView.scrollToPosition(0)
        }
    }

    override fun onPause() {
        viewModel.save()
        super.onPause()
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
            R.id.settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
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
            binding.floatingActionButton.visibility = View.INVISIBLE
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
            binding.floatingActionButton.visibility = View.VISIBLE
            Log.d(TAG, "onDestroyActionMode")
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?) = false
    }

    override fun onClickDeleteButton() {
        Log.d(TAG, "onClickDeleteButton")
        viewModel.deleteSelectedItems()
        actionMode?.finish()
    }

    private fun onClickAddButton() {
        val dialog = EditTitleDialog()
        dialog.arguments = Bundle().apply {
            putInt(EditTitleDialog.KEY_TITLE, R.string.create_new_note)
        }
        dialog.show(supportFragmentManager, EDIT_TITLE_DIALOG)
    }

    override fun onClickOkAtEditTitleDialog(title: String, subTitle: String) {
        Log.d(TAG, "Title = ${title}, SubTitle = $subTitle")
        viewModel.createNewNote(title, subTitle)
        startActivity(Intent(this, NoteActivity::class.java))
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
            val binding = ViewNoteCardBinding.inflate(layoutInflater, parent, false)
            return NoteCardViewHolder(binding, viewModel)
        }

        override fun onBindViewHolder(holder: NoteCardViewHolder, position: Int) {
            holder.bind(position, editMode)
            Log.d(TAG, "onBindViewHolder at $position on $holder")
        }
    }

    class NoteCardViewHolder(private val binding: ViewNoteCardBinding, private val viewModel: NoteListViewModel)
        : RecyclerView.ViewHolder(binding.root) {

        fun bind(position: Int, editMode: Boolean) {
            binding.title.text = viewModel.getTitle(position)
            binding.subTitle.text = viewModel.getSubTitle(position)
            binding.date.text = viewModel.getUpdateDate(position)
            binding.cardView.setOnClickListener {
                if (!editMode) {
                    Log.d(TAG, "onClick Card at $position")
                    viewModel.selectNote(adapterPosition)
                    val intent = Intent(it.context, NoteActivity::class.java)
                    it.context.startActivity(intent)
                }
            }

            /* CheckBox for delete operation */
            binding.checkBox.visibility = if (editMode) View.VISIBLE else View.INVISIBLE
            binding.checkBox.isChecked = viewModel.getSelection(position)
            binding.checkBox.setOnClickListener {
                viewModel.setSelection(adapterPosition, binding.checkBox.isChecked)
            }
        }
    }

    companion object {
        private const val TAG = "NoteListActivity"
        private const val DELETE_CONFIRM_DIALOG = "DeleteConfirmDialog"
        private const val EDIT_TITLE_DIALOG = "EditTitleDialog"
    }
}