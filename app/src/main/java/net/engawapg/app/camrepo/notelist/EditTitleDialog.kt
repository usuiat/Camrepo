package net.engawapg.app.camrepo.notelist

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import net.engawapg.app.camrepo.R

class EditTitleDialog: DialogFragment() {

    private lateinit var listener: EventListener

    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val titleId = arguments?.getInt(KEY_TITLE) ?: 0
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            val inflater = requireActivity().layoutInflater
            builder.setView(inflater.inflate(R.layout.dialog_edit_title, null))
                .setTitle(titleId)
                .setPositiveButton(R.string.ok) { _, _ ->
                    listener.onClickOkAtEditTitleDialog("New Note", "Sub Title")
                }
                .setNegativeButton(R.string.cancel) { _, _ -> Unit }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
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
//        private const val TAG = "EditTitleDialog"
    }
}