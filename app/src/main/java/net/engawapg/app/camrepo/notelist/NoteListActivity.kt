package net.engawapg.app.camrepo.notelist

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_note_list.*
import net.engawapg.app.camrepo.R
import net.engawapg.app.camrepo.note.NoteActivity
import org.koin.android.viewmodel.ext.android.viewModel

class NoteListActivity : AppCompatActivity() {
    private val viewModel: NoteListViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note_list)

        recyclerView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = NoteCardAdapter(viewModel)
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

    class NoteCardAdapter(private val viewModel: NoteListViewModel)
        : RecyclerView.Adapter<NoteCardViewHolder>() {
        override fun getItemCount(): Int {
            return viewModel.getItemCount()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteCardViewHolder {
            val layoutInflater = LayoutInflater.from(parent.context)
            val view = layoutInflater.inflate(R.layout.view_note_card, parent, false)
            return NoteCardViewHolder(view)
        }

        override fun onBindViewHolder(holder: NoteCardViewHolder, position: Int) {
        }
    }

    class NoteCardViewHolder(v: View): RecyclerView.ViewHolder(v)

    companion object {
        private const val TAG = "NoteListActivity"
        private const val RequestCode_NoteActivity = 1
    }
}