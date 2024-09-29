package com.buspljus

import android.app.AlertDialog
import android.content.Context
import android.widget.EditText

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

    fun prikaziGresku(g: Exception) {
        Builder(context).setTitle(R.string.greska_u_programu).setMessage(g.toString()).show()
    }

    fun stackTrace(t: Thread, m: Throwable) {
        Builder(context).setTitle(t.toString()).setMessage(m.toString()).show()
    }

    fun podesavanjeMnozioca() {
        val editText = EditText(context)
        editText.setText(IzracunavanjeVremena.mnozilac.toString())
        Builder(context).setTitle(context.resources.getString(R.string.mnozilac))
            .setView(editText)
            .setPositiveButton("OK") { dialog, which ->
                val unos = editText.text
                try {
                    IzracunavanjeVremena.mnozilac = unos.toString().toDouble()
                    dismiss()
                }
                catch (e: Exception) {
                    Toster(context).toster("Nepravilan broj")
                }
            }
            .create()
            .show()
    }
}