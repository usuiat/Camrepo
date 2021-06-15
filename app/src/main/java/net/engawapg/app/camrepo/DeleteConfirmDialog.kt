package net.engawapg.app.camrepo

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.DialogFragment

class DeleteConfirmDialog: DialogFragment() {

    private var listener: EventListener? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            // Use the Builder class for convenient dialog construction
            val builder = AlertDialog.Builder(it)
            builder.setMessage(R.string.delete_confirm_message)
                .setPositiveButton(R.string.delete) { _, _ ->
                    listener?.onClickDeleteButton()
                }
                .setNegativeButton(R.string.cancel) { _, _ -> }
            // Create the AlertDialog object and return it
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    interface EventListener {
        fun onClickDeleteButton()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = when {
            context is EventListener -> context
            parentFragment is EventListener -> parentFragment as EventListener
            else -> null
        }
    }
}