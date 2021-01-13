package net.engawapg.app.camrepo.notelist

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
import net.engawapg.app.camrepo.R
import net.engawapg.app.camrepo.databinding.DialogEditTitleBinding
import org.koin.android.viewmodel.ext.android.viewModel

class EditTitleDialog: DialogFragment() {

    private val viewModel: EditTitleViewModel by viewModel()

    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        return activity?.let {
            val binding = DataBindingUtil.inflate<DialogEditTitleBinding>(
                LayoutInflater.from(it),
                R.layout.dialog_edit_title,
                null,
                false
            )
            binding.viewModel = viewModel
            val view = binding.root

            val builder = AlertDialog.Builder(it)
            builder.setView(view)
                .setTitle(R.string.edit_note_title)
                .setPositiveButton(R.string.ok) { _, _ ->
                    viewModel.save()
                    findNavController().previousBackStackEntry?.savedStateHandle?.set(
                        KEY_RESULT, RESULT_OK
                    )
                }
                .setNegativeButton(R.string.cancel) { _, _ -> Unit }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    companion object {
        const val KEY_RESULT = "EditTitleDialog_Result"
        const val RESULT_OK = 1
    }
}