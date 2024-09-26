package com.jnj.vaccinetracker.common.dialogs

import android.app.AlertDialog
import android.content.Context

class AlertDialog(
   private val context: Context
) {
   fun showAlertDialog(message: String) {
      val alertDialog = AlertDialog.Builder(context)
         .setMessage(message)
         .setPositiveButton(android.R.string.ok) { dialog, _ ->
            dialog.dismiss()
         }
         .create()

      alertDialog.show()
   }
}