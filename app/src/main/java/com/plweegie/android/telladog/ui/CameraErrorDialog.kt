package com.plweegie.android.telladog.ui

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment


class CameraErrorDialog : DialogFragment() {

    companion object {
        private const val ARG_MESSAGE = "message"

        @JvmStatic
        fun newInstance(message: String): CameraErrorDialog {
            val dialog = CameraErrorDialog()
            val args = Bundle().apply { putString(ARG_MESSAGE, message) }
            return dialog.apply { arguments = args }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
            AlertDialog.Builder(activity)
                    .setMessage(arguments?.getString(ARG_MESSAGE))
                    .setPositiveButton(android.R.string.ok) { _, _ -> activity?.finish() }
                    .create()
}