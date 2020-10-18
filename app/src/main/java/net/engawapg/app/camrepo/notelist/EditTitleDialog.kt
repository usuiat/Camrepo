package net.engawapg.app.camrepo.notelist

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import kotlinx.android.synthetic.main.dialog_edit_title.view.*
import net.engawapg.app.camrepo.R

class EditTitleDialog: DialogFragment() {

    private lateinit var listener: EventListener

    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val titleId = arguments?.getInt(KEY_TITLE) ?: 0
        val noteTitle = arguments?.getString(KEY_NOTE_TITLE) ?: ""
        val noteSubTitle = arguments?.getString(KEY_NOTE_SUB_TITLE) ?: ""

        return activity?.let {
            val inflater = requireActivity().layoutInflater
            val view = inflater.inflate(R.layout.dialog_edit_title, null)
            view.editTitle.setText(noteTitle)
            view.editSubTitle.setText(noteSubTitle)

            val builder = AlertDialog.Builder(it)
            builder.setView(view)
                .setTitle(titleId)
                .setPositiveButton(R.string.ok) { _, _ ->
                    onClickOk()
                }
                .setNegativeButton(R.string.cancel) { _, _ -> Unit }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    private fun onClickOk() {
        val et = dialog?.findViewById<EditText>(R.id.editTitle)
        val est = dialog?.findViewById<EditText>(R.id.editSubTitle)
        var title = et?.text.toString()
        if (title == "") title = getString(R.string.default_note_title)
        val subTitle = est?.text.toString() /* subTitleは未入力の場合は空欄にしておく */

        listener.onClickOkAtEditTitleDialog(title, subTitle)
    }

    interface EventListener {
        fun onClickOkAtEditTitleDialog(title: String, subTitle: String)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            listener = context as EventListener
        } catch (e: ClassCastException) {
            throw ClassCastException((context.toString() + "must implement Listener."))
        }
    }



    companion object {
        const val KEY_TITLE = "KeyTitle"
        const val KEY_NOTE_TITLE = "KeyNoteTitle"
        const val KEY_NOTE_SUB_TITLE = "KeyNoteSubTitle"
//        private const val TAG = "EditTitleDialog"
    }
}