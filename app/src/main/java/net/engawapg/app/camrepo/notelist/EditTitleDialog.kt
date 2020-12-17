package net.engawapg.app.camrepo.notelist

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import net.engawapg.app.camrepo.R
import net.engawapg.app.camrepo.databinding.DialogEditTitleBinding
import org.koin.android.viewmodel.ext.android.sharedViewModel

class EditTitleDialog: DialogFragment() {

    private val viewModel: EditTitleViewModel by sharedViewModel()

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
                .setTitle(viewModel.dialogTitle)
                .setPositiveButton(R.string.ok) { _, _ ->
                    onClickOk()
                }
                .setNegativeButton(R.string.cancel) { _, _ -> Unit }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    private fun onClickOk() {
//        val et = dialog?.findViewById<EditText>(R.id.editTitle)
//        val est = dialog?.findViewById<EditText>(R.id.editSubTitle)
//        viewModel.title = et?.text.toString()
        if (viewModel.title == "") viewModel.title = getString(R.string.default_note_title)
//        viewModel.subTitle = est?.text.toString() /* subTitleは未入力の場合は空欄にしておく */

        viewModel.onClickOk.value = true
    }
}