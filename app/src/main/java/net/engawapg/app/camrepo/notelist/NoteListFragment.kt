package net.engawapg.app.camrepo.notelist

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.observe
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_note_list.*
import kotlinx.android.synthetic.main.view_note_card.view.*
import net.engawapg.app.camrepo.DeleteConfirmDialog
import net.engawapg.app.camrepo.R
import org.koin.android.viewmodel.ext.android.sharedViewModel

class NoteListFragment : Fragment() {

    private val viewModel: NoteListViewModel by sharedViewModel()
    private val editTitleViewModel: EditTitleViewModel by sharedViewModel()
    private lateinit var noteCardAdapter: NoteCardAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_note_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        noteCardAdapter = NoteCardAdapter(viewModel)
        recyclerView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = noteCardAdapter
        }

        /* Get a NavController */
        val navHostFragment = parentFragmentManager.findFragmentById(R.id.nav_host_fragment)
                as NavHostFragment
        val navController = navHostFragment.navController

        editNoteListButton.setOnClickListener{ onClickEditNoteListButton() }
        closeEditModeButton.setOnClickListener{ onClickCloseEditModeButton() }
        deleteButton.setOnClickListener { onClickDeleteButton() }
        /* Observe result from DeleteConfirmDialog */
        navController.currentBackStackEntry?.savedStateHandle
            ?.getLiveData<Int>(DeleteConfirmDialog.KEY_RESULT)
            ?.observe(viewLifecycleOwner) { result ->
                onDeleteConfirmDialogResult(result)
            }

        floatingActionButton.setOnClickListener { onClickAddButton() }

        editTitleViewModel.onClickOk.observe(viewLifecycleOwner, Observer {
            viewModel.createNewNote(editTitleViewModel.title, editTitleViewModel.subTitle)
            noteCardAdapter.notifyDataSetChanged()
        })
    }

    private fun onClickEditNoteListButton() {
        viewModel.initSelection()
        noteCardAdapter.editMode = true
        editModeBar.visibility = View.VISIBLE
    }

    private fun onClickCloseEditModeButton() {
        viewModel.clearSelection()
        noteCardAdapter.editMode = false
        editModeBar.visibility = View.INVISIBLE
    }

    private fun onClickDeleteButton() {
        if (viewModel.isSelected()) {
            findNavController().navigate(R.id.action_noteFragment_to_deleteConfirmDialog)
        } else {
            onClickCloseEditModeButton()
        }
    }

    private fun onDeleteConfirmDialogResult(result: Int) {
        Log.d(TAG, "onDeleteConfirmDialogResult: $result")
        if (result == DeleteConfirmDialog.RESULT_DELETE) {
            viewModel.deleteSelectedItems()
            onClickCloseEditModeButton()
            viewModel.save()
        }
    }

    private fun onClickAddButton() {
        editTitleViewModel.apply {
            dialogTitle = getString(R.string.create_new_note)
            title = ""
            subTitle = ""
        }
        findNavController().navigate(R.id.action_noteFragment_to_editTitleDialog)
    }

    companion object {
        private const val TAG = "NoteListFragment"
//        @JvmStatic
//        fun newInstance() = NoteListFragment()
    }

    class NoteCardAdapter(private val viewModel: NoteListViewModel)
        : RecyclerView.Adapter<NoteCardViewHolder>() {

        var editMode = false
            set(value) {
                field = value
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
            itemView.date.text = viewModel.getUpdateDate(position)
            itemView.cardView.setOnClickListener {
                if (!editMode) {
                    Log.d(TAG, "onClick Card at $position")
                    viewModel.selectNote(adapterPosition)
                }
            }

            /* CheckBox for delete operation */
            itemView.checkBox.visibility = if (editMode) View.VISIBLE else View.GONE
            itemView.checkBox.isChecked = viewModel.getSelection(position)
            itemView.checkBox.setOnClickListener {
                viewModel.setSelection(adapterPosition, itemView.checkBox.isChecked)
            }
        }
    }
}