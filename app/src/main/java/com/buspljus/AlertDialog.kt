package com.buspljus

import android.app.AlertDialog
import android.content.Context

class AlertDialog(private val context: Context): AlertDialog(context) {
    fun pronadjeneStaniceAlertDialog(pronadjeneStanice: List<String>, callback: Interfejs.Callback) {
        if (pronadjeneStanice.size > 1) {
            Builder(context)
                .setTitle(context.resources.getString(R.string.pronadjene_stanice))
                .setItems(pronadjeneStanice.toTypedArray()) { dialog, which ->
                    callback.korak(pronadjeneStanice[which])
                    dialog.dismiss()
                }
                .show()


        }
        else if (pronadjeneStanice.size == 1) {
            Builder(context)
                .setTitle(context.resources.getString(R.string.prihvatiti_stanicu))
                .setMessage(pronadjeneStanice[0])
                .setPositiveButton(context.resources.getString(R.string.da)) { dialog, _ ->
                    callback.korak(pronadjeneStanice[0])
                    dialog.dismiss()
                }
                .show()
        }

    }

    fun preuzimanjeMapeiliStanica(bazaMapa: Int, velicina: Double, odgovor: Interfejs.odgovor) {
        Builder(context)
            .setTitle(if (bazaMapa == 0) context.resources.getString(R.string.azurirati_mapu) else context.resources.getString(R.string.azurirati_bazu))
            .setMessage(context.resources.getString(R.string.potrebnopreuzeti) + " " + VoziloInfo(context).zaokruzivanje(velicina/1048576, 2) + "MB")
            .setPositiveButton(context.resources.getString(R.string.da)) { dialog, _ ->
                odgovor.da(true)
                dialog.dismiss()
            }
            .show()
    }
}