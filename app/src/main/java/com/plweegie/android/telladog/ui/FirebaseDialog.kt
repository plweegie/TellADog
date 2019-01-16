package com.plweegie.android.telladog.ui

import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.app.AlertDialog
import android.support.v4.content.res.ResourcesCompat
import android.view.LayoutInflater
import com.plweegie.android.telladog.R
import kotlinx.android.synthetic.main.dialog_firebase.view.*

class FirebaseDialog : DialogFragment() {

    internal lateinit var listener: FirebaseDialogListener

    interface FirebaseDialogListener {
        fun onPositiveClick(dialog: DialogFragment, isPermanent: Boolean)
        fun onNegativeClick(dialog: DialogFragment, isPermanent: Boolean)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            val dialog = LayoutInflater.from(context).inflate(R.layout.dialog_firebase, null).apply {
                firebase_dialog_check.typeface = ResourcesCompat.getFont(it, R.font.open_sans)
            }

            builder.setView(dialog)
                    .setPositiveButton(R.string.dialog_yes) { _, _ ->
                        listener.onPositiveClick(this, dialog.firebase_dialog_check.isChecked)
                    }
                    .setNegativeButton(R.string.dialog_no) { _, _ ->
                        if (dialog.firebase_dialog_check.isChecked) {
                            listener.onNegativeClick(this, dialog.firebase_dialog_check.isChecked)
                        }
                    }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}