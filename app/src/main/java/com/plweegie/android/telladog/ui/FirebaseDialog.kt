package com.plweegie.android.telladog.ui

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.DialogFragment
import com.plweegie.android.telladog.R
import com.plweegie.android.telladog.databinding.DialogFirebaseBinding

class FirebaseDialog : DialogFragment() {

    internal lateinit var listener: FirebaseDialogListener

    private lateinit var binding: DialogFirebaseBinding

    interface FirebaseDialogListener {
        fun onPositiveClick(dialog: DialogFragment, isPermanent: Boolean)
        fun onNegativeClick(dialog: DialogFragment, isPermanent: Boolean)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            binding = DialogFirebaseBinding.inflate(LayoutInflater.from(context)).apply {
                firebaseDialogCheck.typeface = ResourcesCompat.getFont(it, R.font.open_sans)
            }
            val builder = AlertDialog.Builder(it)

            builder.setView(binding.root)
                    .setPositiveButton(R.string.dialog_yes) { _, _ ->
                        listener.onPositiveClick(this, binding.firebaseDialogCheck.isChecked)
                    }
                    .setNegativeButton(R.string.dialog_no) { _, _ ->
                        if (binding.firebaseDialogCheck.isChecked) {
                            listener.onNegativeClick(this, binding.firebaseDialogCheck.isChecked)
                        }
                    }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}