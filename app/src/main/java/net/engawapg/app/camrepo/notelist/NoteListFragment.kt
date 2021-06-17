package net.engawapg.app.camrepo.notelist

import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.findFragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.engawapg.app.camrepo.DeleteConfirmDialog
import net.engawapg.app.camrepo.R
import net.engawapg.app.camrepo.databinding.FragmentNoteListBinding
import net.engawapg.app.camrepo.databinding.ViewNoteCardBinding
import org.koin.android.viewmodel.ext.android.viewModel

class NoteListFragment: Fragment(), DeleteConfirmDialog.EventListener{

    companion object {
        private const val DELETE_CONFIRM_DIALOG = "DeleteConfirmDialog"
    }

    private var _binding: FragmentNoteListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: NoteListViewModel by viewModel()
    private var actionMode: ActionMode? = null
    private lateinit var noteCardAdapter: NoteCardAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNoteListBinding.inflate(inflater, container, false)

        noteCardAdapter = NoteCardAdapter(viewModel)
        binding.recyclerView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = noteCardAdapter
        }

        binding.floatingActionButton.setOnClickListener { onClickAddButton() }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_note_list, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.edit_list_items -> {
                actionMode = activity?.startActionMode(actionModeCallback)
                true
            }
            R.id.settings -> {
                findNavController().navigate(R.id.action_noteListFragment_to_settingsFragment)
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
                    DeleteConfirmDialog().show(childFragmentManager, DELETE_CONFIRM_DIALOG)
                }
            }
            return true
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            actionMode = null
            viewModel.clearSelection()
            noteCardAdapter.setEditMode(false)
            binding.floatingActionButton.visibility = View.VISIBLE
            Log.d("NoteListFragment", "onDestroyActionMode")
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?) = false
    }

    override fun onClickDeleteButton() {
        viewModel.deleteSelectedItems()
        actionMode?.finish()
    }

    private fun onClickAddButton() {
        viewModel.createNewNote(getString(R.string.default_note_title), "")
        findNavController().navigate(R.id.action_noteListFragment_to_noteFragment)
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
            Log.d("NoteListFragment", "onBindViewHolder at $position on $holder")
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
                    Log.d("NoteListFragment", "onClick Card at $position")
                    viewModel.selectNote(adapterPosition)
                    it.findFragment<NoteListFragment>().findNavController()
                        .navigate(R.id.action_noteListFragment_to_noteFragment)
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
}