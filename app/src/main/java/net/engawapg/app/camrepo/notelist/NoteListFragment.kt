package net.engawapg.app.camrepo.notelist

import android.content.Intent
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
import net.engawapg.app.camrepo.settings.SettingsActivity
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

        noteCardAdapter = NoteCardAdapter(viewModel, noteCardAdapterListener)
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
        settingButton.setOnClickListener { onClickSettingButton() }
        /* Observe result from DeleteConfirmDialog */
        navController.currentBackStackEntry?.savedStateHandle
            ?.getLiveData<Int>(DeleteConfirmDialog.KEY_RESULT)
            ?.observe(viewLifecycleOwner) { result ->
                onDeleteConfirmDialogResult(result)
            }

        editTitleViewModel.onClickOk.observe(viewLifecycleOwner, Observer {
            if (editTitleViewModel.tag == TAG) {
                viewModel.createNewNote(editTitleViewModel.title, editTitleViewModel.subTitle)
                noteCardAdapter.notifyDataSetChanged()
            }
        })

        viewModel.updateIndex.observe(viewLifecycleOwner, Observer {
            noteCardAdapter.notifyNoteAtIndexChanged(it)
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

    private fun onClickSettingButton() {
        startActivity(Intent(context, SettingsActivity::class.java))
    }

    private val noteCardAdapterListener = object: NoteCardAdapterListener {
        override fun onCreateNewNote() {
            editTitleViewModel.apply {
                dialogTitle = getString(R.string.create_new_note)
                title = ""
                subTitle = ""
                tag = TAG
            }
            findNavController().navigate(R.id.action_noteFragment_to_editTitleDialog)
        }

        override fun onSelectNote(index: Int) {
            viewModel.selectNote(index)
        }
    }

    companion object {
        private const val TAG = "NoteListFragment"
//        @JvmStatic
//        fun newInstance() = NoteListFragment()
    }

    interface NoteCardAdapterListener {
        fun onCreateNewNote()
        fun onSelectNote(index: Int)
    }

    class NoteCardAdapter(private val viewModel: NoteListViewModel,
                          private val listener: NoteCardAdapterListener)
        : RecyclerView.Adapter<NoteCardViewHolder>() {

        var editMode = false
            set(value) {
                field = value
                notifyDataSetChanged()
            }

        override fun getItemCount(): Int {
            var count = viewModel.getItemCount()
            if (!editMode) count++  /* 新規ノート作成 */
            return count
        }

        fun notifyNoteAtIndexChanged(index: Int) {
            var i = index
            if (!editMode) i++
            notifyItemChanged(i)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteCardViewHolder {
            val layoutInflater = LayoutInflater.from(parent.context)
            val view = layoutInflater.inflate(R.layout.view_note_card, parent, false)
            return NoteCardViewHolder(view, viewModel)
        }

        override fun onBindViewHolder(holder: NoteCardViewHolder, position: Int) {
            if ((!editMode) and (position == 0)) {
                holder.bindNewNote()
                holder.itemView.cardView.setOnClickListener { listener.onCreateNewNote() }
            } else {
                var itemPosition = position
                if (!editMode) itemPosition--
                holder.bind(itemPosition, editMode)
                holder.itemView.cardView.setOnClickListener {
                    if (!editMode) {
                        listener.onSelectNote(itemPosition)
                    }
                }
            }
        }
    }

    class NoteCardViewHolder(v: View, private val viewModel: NoteListViewModel)
        : RecyclerView.ViewHolder(v) {

        fun bind(position: Int, editMode: Boolean) {
            itemView.title.text = viewModel.getTitle(position)
            itemView.date.apply {
                visibility = View.VISIBLE
                text = viewModel.getUpdateDate(position)
            }
            itemView.cardView.setOnClickListener {
                if (!editMode) {
                    Log.d(TAG, "onClick Card at $position")
                    viewModel.selectNote(adapterPosition)
                }
            }
            itemView.noteIcon.apply {
                visibility = View.GONE
                setImageResource(android.R.color.white)
            }

            /* CheckBox for delete operation */
            itemView.checkBox.visibility = if (editMode) View.VISIBLE else View.GONE
            itemView.checkBox.isChecked = viewModel.getSelection(position)
            itemView.checkBox.setOnClickListener {
                viewModel.setSelection(adapterPosition, itemView.checkBox.isChecked)
            }
        }

        fun bindNewNote() {
            itemView.title.text = itemView.context.getString(R.string.create_new_note)
            itemView.date.visibility = View.GONE
            itemView.checkBox.visibility = View.GONE
            itemView.noteIcon.apply {
                setImageResource(R.drawable.add)
                visibility = View.VISIBLE
            }
        }
    }
}