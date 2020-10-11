package net.engawapg.app.camrepo.page

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import net.engawapg.app.camrepo.R

class PageTitleDialog: DialogFragment() {

    private lateinit var listener: EventListener

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            AlertDialog.Builder(it)
                .setTitle(R.string.page_title)
                .setView(R.layout.dialog_page_title)
                .setPositiveButton(R.string.ok) { _, _ ->
                    listener.onClickOkAtPageTitleDialog("New Page")
                }
                .setNegativeButton(R.string.cancel) { _, _ -> Unit }
                .create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    interface EventListener {
        fun onClickOkAtPageTitleDialog(title: String)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            listener = context as EventListener
        } catch (e: ClassCastException) {
            throw ClassCastException((context.toString() + "must implement Listener."))
        }
    }
}