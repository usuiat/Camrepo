package net.engawapg.app.camrepo.notelist

import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_note_list.*
import net.engawapg.app.camrepo.DeleteConfirmDialog
import net.engawapg.app.camrepo.R
import net.engawapg.app.camrepo.databinding.FragmentNoteListBinding
import net.engawapg.app.camrepo.databinding.ViewNoteCardBinding
import org.koin.android.viewmodel.ext.android.sharedViewModel

class NoteListFragment : Fragment() {

    private val viewModel: NoteListViewModel by sharedViewModel()
    private lateinit var noteCardAdapter: NoteCardAdapter
    private var actionMode: ActionMode? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = DataBindingUtil.inflate<FragmentNoteListBinding>(
            inflater, R.layout.fragment_note_list, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        setHasOptionsMenu(true)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        /* Toolbar */
        (activity as AppCompatActivity?)?.supportActionBar?.setLogo(R.drawable.ic_logo)

        noteCardAdapter = NoteCardAdapter(viewModel, viewLifecycleOwner)
        recyclerView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = noteCardAdapter
        }

        viewModel.onSelectNote.observe(viewLifecycleOwner, Observer {
            onSelectNote(it)
        })
        viewModel.onCreateNote.observe(viewLifecycleOwner, Observer {
            onCreateNote(it)
        })

        /* Observe result from DeleteConfirmDialog */
        findNavController().currentBackStackEntry?.savedStateHandle
            ?.getLiveData<Int>(DeleteConfirmDialog.KEY_RESULT)
            ?.observe(viewLifecycleOwner) { result ->
                onDeleteConfirmDialogResult(result)
            }
    }

    override fun onPause() {
        viewModel.save()
        super.onPause()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_note_list, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.edit_list_items -> {
                actionMode = activity?.startActionMode(actionModeCallback)
                true
            }
            R.id.settings -> {
                findNavController().navigate(R.id.action_noteListFragment_to_settingsActivity)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private val actionModeCallback = object: ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            mode?.menuInflater?.inflate(R.menu.menu_note_list_action_mode, menu)
            viewModel.setEditMode(true)
            noteCardAdapter.notifyDataSetChanged()
            return true
        }

        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
            if (item?.itemId == R.id.delete_selected_items) {
                if (viewModel.isSelected()) {
                    findNavController().navigate(R.id.action_global_deleteConfirmDialog)
                }
            }
            return true
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            viewModel.setEditMode(false)
            noteCardAdapter.notifyDataSetChanged()
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            return false
        }
    }

    private fun onDeleteConfirmDialogResult(result: Int) {
        Log.d(TAG, "onDeleteConfirmDialogResult: $result")
        if (result == DeleteConfirmDialog.RESULT_DELETE) {
            viewModel.deleteSelectedItems()
            actionMode?.finish()
        }
    }

    private fun onSelectNote(fileName: String) {
        Log.d(TAG, "onSelectNote fileName=$fileName")
    }

    private fun onCreateNote(fileName: String) {
        Log.d(TAG, "onCreateNote fileName=$fileName")
        noteCardAdapter.notifyItemInserted(0)
    }

    companion object {
        private const val TAG = "NoteListFragment"
//        @JvmStatic
//        fun newInstance() = NoteListFragment()
    }

    class NoteCardAdapter(private val viewModel: NoteListViewModel,
                          private val lifecycleOwner: LifecycleOwner)
        : RecyclerView.Adapter<NoteCardViewHolder>() {

        override fun getItemCount(): Int {
            return viewModel.getItemCount()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteCardViewHolder {
            val layoutInflater = LayoutInflater.from(parent.context)
            val binding = DataBindingUtil.inflate<ViewNoteCardBinding>(
                layoutInflater, R.layout.view_note_card, parent, false)
            binding.lifecycleOwner = lifecycleOwner
            binding.viewModel = viewModel
            return NoteCardViewHolder(binding)
        }

        override fun onBindViewHolder(holder: NoteCardViewHolder, position: Int) {
            holder.binding.item = viewModel.getItem(position)
            holder.binding.executePendingBindings()
        }
    }

    class NoteCardViewHolder(val binding: ViewNoteCardBinding)
        : RecyclerView.ViewHolder(binding.root)
}