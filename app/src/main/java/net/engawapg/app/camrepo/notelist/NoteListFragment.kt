package net.engawapg.app.camrepo.notelist

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_note_list.*
import kotlinx.android.synthetic.main.view_note_card.view.*
import net.engawapg.app.camrepo.R
import net.engawapg.app.camrepo.note.NoteActivity
import org.koin.android.viewmodel.ext.android.sharedViewModel

/**
 * A simple [Fragment] subclass.
 * Use the [NoteListFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class NoteListFragment : Fragment(), EditTitleDialog.EventListener {

    private val viewModel: NoteListViewModel by sharedViewModel()
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

        floatingActionButton.setOnClickListener { onClickAddButton() }
    }

    private fun onClickAddButton() {
        val dialog = EditTitleDialog()
        dialog.arguments = Bundle().apply {
            putInt(EditTitleDialog.KEY_TITLE, R.string.create_new_note)
        }
        dialog.show(parentFragmentManager, EDIT_TITLE_DIALOG)
    }

    override fun onClickOkAtEditTitleDialog(title: String, subTitle: String) {
        Log.d(TAG, "Title = ${title}, SubTitle = $subTitle")
        viewModel.createNewNote(title, subTitle)
//        startActivity(Intent(this, NoteActivity::class.java))
    }

    companion object {
        private const val TAG = "NoteListFragment"
        private const val EDIT_TITLE_DIALOG = "EditTitleDialog"
        @JvmStatic
        fun newInstance() = NoteListFragment()
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
                    viewModel.selectNote(adapterPosition)
//                    val intent = Intent(it.context, NoteActivity::class.java)
//                    it.context.startActivity(intent)
                }
            }

            /* CheckBox for delete operation */
            itemView.checkBox.visibility = if (editMode) View.VISIBLE else View.INVISIBLE
            itemView.checkBox.isChecked = viewModel.getSelection(position)
            itemView.checkBox.setOnClickListener {
                viewModel.setSelection(adapterPosition, itemView.checkBox.isChecked)
            }
        }
    }
}