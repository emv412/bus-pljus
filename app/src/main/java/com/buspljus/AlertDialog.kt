package com.buspljus

import android.app.AlertDialog
import android.content.Context
import android.widget.ListView
import android.widget.TextView
import com.buspljus.SQLcitac.Companion.pronadjeneStanice
import com.google.android.material.bottomsheet.BottomSheetDialog

class AlertDialog(private val context: Context): AlertDialog(context) {
    fun pronadjeneStaniceAlertDialog(callback: SQLcitac.Callback) {
        if (pronadjeneStanice.size > 1) {
            /*
            val dial = BottomSheetDialog(context)
            dial.setTitle(context.resources.getString(R.string.pronadjene_stanice))
            val lista = dial.setContentView(R.layout.probna_lista)
            val sifra = dial.findViewById<TextView>(R.id.sifra_stanice)
            val nazivi = dial.findViewById<TextView>(R.id.naziv_stanice)
            val odredista = dial.findViewById<TextView>(R.id.odredista_sa_stanice)

            val l = ListView(context)
            l.addView(pronadjeneStanice)

             */


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
}