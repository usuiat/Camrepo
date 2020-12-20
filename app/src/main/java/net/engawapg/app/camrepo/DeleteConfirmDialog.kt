package net.engawapg.app.camrepo

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController

class DeleteConfirmDialog: DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            // Use the Builder class for convenient dialog construction
            val builder = AlertDialog.Builder(it)
            builder.setMessage(R.string.delete_confirm_message)
                .setPositiveButton(R.string.delete) { _, _ ->
                    findNavController().previousBackStackEntry?.savedStateHandle?.set(
                        KEY_RESULT, RESULT_DELETE
                    )
                }
                .setNegativeButton(R.string.cancel) { _, _ -> Unit }
            // Create the AlertDialog object and return it
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    companion object {
        const val KEY_RESULT = "DialogFragment_Result"
        const val RESULT_DELETE = 1
    }
}