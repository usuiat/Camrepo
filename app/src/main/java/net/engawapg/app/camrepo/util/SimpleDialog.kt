package net.engawapg.app.camrepo.util

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.DialogFragment

class SimpleDialog : DialogFragment() {

    interface ResultListener {
        fun onSimpleDialogResult(tag: String?, result: Result)
    }

    enum class Result {
        POSITIVE,
        NEGATIVE
    }

    class Builder {
        private val bundle = Bundle()

        fun setTitle(title: String) = this.apply {
            bundle.putString(KEY_TITLE, title)
        }

        fun setTitle(titleId: Int) = this.apply {
            bundle.putInt(KEY_TITLE_ID, titleId)
        }

        fun setMessage(message: String) = this.apply {
            bundle.putString(KEY_MESSAGE, message)
        }

        fun setMessage(messageId: Int) = this.apply {
            bundle.putInt(KEY_MESSAGE_ID, messageId)
        }

        fun setPositiveText(text: String) = this.apply {
            bundle.putString(KEY_POSITIVE_TEXT, text)
        }

        fun setPositiveText(textId: Int) = this.apply {
            bundle.putInt(KEY_POSITIVE_TEXT_ID, textId)
        }

        fun setNegativeText(text: String) = this.apply {
            bundle.putString(KEY_NEGATIVE_TEXT, text)
        }

        fun setNegativeText(textId: Int) = this.apply {
            bundle.putInt(KEY_NEGATIVE_TEXT_ID, textId)
        }

        fun create() = SimpleDialog().apply {
            arguments = bundle
        }
    }

    companion object {
        private const val KEY_TITLE = "KeyTitle"
        private const val KEY_TITLE_ID = "KeyTitleId"
        private const val KEY_MESSAGE = "KeyMessage"
        private const val KEY_MESSAGE_ID = "KeyMessageId"
        private const val KEY_POSITIVE_TEXT = "KeyPositiveText"
        private const val KEY_POSITIVE_TEXT_ID = "KeyPositiveTextId"
        private const val KEY_NEGATIVE_TEXT = "KeyNegativeText"
        private const val KEY_NEGATIVE_TEXT_ID = "KeyNegativeTextId"
    }

    private var listener: ResultListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = when {
            context is ResultListener -> context
            parentFragment is ResultListener -> parentFragment as ResultListener
            else -> null
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val builder = activity?.let { AlertDialog.Builder(it) }
            ?: throw IllegalStateException("Activity is null when onCreateDialog() is called.")

        /* Title */
        val titleId = arguments?.getInt(KEY_TITLE_ID) ?: 0
        if (titleId != 0) {
            builder.setTitle(titleId)
        } else {
            builder.setTitle(arguments?.getString(KEY_TITLE))
        }

        /* Message */
        val messageId = arguments?.getInt(KEY_MESSAGE_ID) ?: 0
        if (messageId != 0) {
            builder.setMessage(messageId)
        } else {
            builder.setMessage(arguments?.getString(KEY_MESSAGE))
        }

        /* Positive Button */
        val positiveTextId = arguments?.getInt(KEY_POSITIVE_TEXT_ID) ?: 0
        if (positiveTextId != 0) {
            builder.setPositiveButton(positiveTextId) { _,_ ->
                listener?.onSimpleDialogResult(tag, Result.POSITIVE)
            }
        } else {
            builder.setPositiveButton(arguments?.getString(KEY_POSITIVE_TEXT)) { _,_ ->
                listener?.onSimpleDialogResult(tag, Result.POSITIVE)
            }
        }

        /* Negative Button */
        val negativeTextId = arguments?.getInt(KEY_NEGATIVE_TEXT_ID) ?: 0
        if (negativeTextId != 0) {
            builder.setNegativeButton(negativeTextId) { _,_ ->
                listener?.onSimpleDialogResult(tag, Result.NEGATIVE)
            }
        } else {
            builder.setNegativeButton(arguments?.getString(KEY_NEGATIVE_TEXT)) { _,_ ->
                listener?.onSimpleDialogResult(tag, Result.NEGATIVE)
            }
        }

        return builder.create()
    }
}