package com.buspljus.grafika

import android.app.AlertDialog
import android.content.Context
import android.database.Cursor
import android.util.Log
import android.widget.EditText
import com.buspljus.Adapteri.PretragaStanica
import com.buspljus.Alati
import com.buspljus.Baza.PosrednikBaze
import com.buspljus.Interfejs
import com.buspljus.IzracunavanjeVremena
import com.buspljus.R
import com.buspljus.Toster

class PopupProzor(private val context: Context): AlertDialog(context) {
    fun pronadjeneStaniceAlertDialog(kursor: Cursor, callback: Interfejs.MapTapListener) {
        if (kursor.count == 0) {
            kursor.close()
            return
        }


        val ad = PretragaStanica(context, kursor)

        val dialog = Builder(context)
            .setTitle(R.string.pronadjene_stanice)
            .setAdapter(ad) { d, which ->
                if (kursor.moveToPosition(which)) {
                    val sifra = kursor.getString(kursor.getColumnIndexOrThrow(PosrednikBaze.Companion.ID_KOLONA))
                    val naziv = kursor.getString(kursor.getColumnIndexOrThrow(PosrednikBaze.Companion.CIR_KOLONA))
                    callback.podesiTextView(sifra, naziv)
                }
                d.dismiss()
            }
            .setNegativeButton(R.string.ponisti, null)
            .create()

        dialog.setOnDismissListener {
            kursor.close()
        }

        dialog.show()
    }

    fun preuzimanjeMapeiliStanica(bazaMapa: Int, velicina: Double, odgovor: Interfejs.odgovor) {
        Builder(context)
            .setTitle(if (bazaMapa == 0) context.resources.getString(R.string.azurirati_mapu) else context.resources.getString(
                R.string.azurirati_bazu))
            .setMessage(context.resources.getString(R.string.potrebnopreuzeti) + " " + Alati(
                context
            ).zaokruzivanje(velicina/1048576, 2) + "MB")
            .setPositiveButton(context.resources.getString(R.string.da)) { dialog, _ ->
                odgovor.da(true)
                dialog.dismiss()
            }
            .show()
    }

    fun prikaziGresku(g: Exception) {
        val poruka = Log.getStackTraceString(g)
        setTitle(R.string.greska_u_programu)
        setMessage(poruka)
        setButton(BUTTON_POSITIVE, "OK") { _, _ -> dismiss() }
        show()
    }

    fun stackTrace(t: Thread, m: Throwable) {
        Builder(context).setTitle(t.toString()).setMessage(Log.getStackTraceString(m)).show()
    }

    fun podesavanjeMnozioca() {
        val editText = EditText(context)
        editText.setText(IzracunavanjeVremena.Companion.mnozilac.toString())
        Builder(context).setTitle(context.resources.getString(R.string.mnozilac))
            .setView(editText)
            .setPositiveButton("OK") { dialog, which ->
                val unos = editText.text
                try {
                    IzracunavanjeVremena.Companion.mnozilac = unos.toString().toDouble()
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