package com.buspljus

import android.app.AlertDialog
import android.content.Context
import com.buspljus.SQLcitac.Companion.pronadjeneStanice

class AlertDialog(private val context: Context): AlertDialog(context) {
    fun pronadjeneStaniceAlertDialog(callback: SQLcitac.Callback) {
        if (pronadjeneStanice.size > 1) {
            Builder(context)
                .setTitle(context.resources.getString(R.string.pronadjene_stanice))
                .setItems(pronadjeneStanice.toTypedArray()) { dialog, which ->
                    callback.korak1(pronadjeneStanice[which])
                    dialog.dismiss()
                }
                .show()
        }
        else if (pronadjeneStanice.size == 1) {
            Builder(context)
                .setTitle(context.resources.getString(R.string.prihvatiti_stanicu))
                .setMessage(pronadjeneStanice[0])
                .setPositiveButton(context.resources.getString(R.string.da)) { dialog, _ ->
                    callback.korak1(pronadjeneStanice[0])
                    dialog.dismiss()
                }
                .show()
        }

    }
}