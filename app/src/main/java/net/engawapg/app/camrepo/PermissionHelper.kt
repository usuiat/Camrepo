package net.engawapg.app.camrepo

import android.app.AlertDialog
import android.app.Dialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment

class PermissionHelper(
    private val fragment: Fragment,
    private val permissions: Array<String>,
    private val confirmationMessage: String
) {
    private var hasRequestedPermission: Boolean = false

    fun needToRequest(): Boolean {
        val ctx = fragment.context?: return true

        for (perm in permissions) {
            if (ContextCompat.checkSelfPermission(ctx, perm) != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Permission is NOT granted - $perm")
                return true
            }
            else {
                Log.d(TAG, "Permission is granted - $perm")
            }
        }
        return false
    }

    fun request() {
        var showRationale = false
        for (perm in permissions) {
            showRationale = showRationale.or(fragment.shouldShowRequestPermissionRationale(perm))
        }

        when {
            showRationale -> {
                /* 一度拒否したが、「今後は表示しない」にはチェックしていない */
                Log.d(TAG, "Show Request Permission Rationale.")
                val args = Bundle().apply {
                    putString(ConfirmationDialog.ARG_MESSAGE, confirmationMessage)
                    putStringArray(ConfirmationDialog.ARG_PERMISSIONS, permissions)
                }
                ConfirmationDialog().apply {
                    arguments = args
                    show(fragment.childFragmentManager, CONFIRM_DIALOG)
                }
            }
            hasRequestedPermission -> {
                /* 「今後は表示しない」にチェックしている */
                Log.d(TAG, "Permissions are not granted finally.")
                // TODO: callback to fragment
            }
            else -> {
                /* 初回 */
                Log.d(TAG, "Request permissions - ${permissions.joinToString()}")
                fragment.requestPermissions(permissions, REQUEST_CODE)
                hasRequestedPermission = true
            }
        }
    }

    class ConfirmationDialog : DialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val message = arguments?.getString(ARG_MESSAGE)
            val permissions = arguments?.getStringArray(ARG_PERMISSIONS)
            return activity?.let {
                // Use the Builder class for convenient dialog construction
                val builder = AlertDialog.Builder(it)
                builder.setMessage(message)
                    .setPositiveButton(R.string.ok) { _, _ ->
                        if (permissions != null) { requestPermissions(permissions, REQUEST_CODE) }
                    }
                    .setNegativeButton(R.string.cancel) { _, _ ->
                        Log.d(TAG, "Permissions are not granted finally.")
                        // TODO: callback to fragment
                    }
                // Create the AlertDialog object and return it
                builder.create()
            } ?: throw IllegalStateException("Activity cannot be null")
        }
        companion object {
            const val ARG_MESSAGE = "message"
            const val ARG_PERMISSIONS = "permissions"
        }
    }

    companion object {
        private const val TAG = "PermissionHelper"
        private const val REQUEST_CODE = 1
        private const val CONFIRM_DIALOG = "confirm dialog"
    }
}