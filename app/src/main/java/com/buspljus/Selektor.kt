package com.buspljus

import android.database.Cursor
import android.view.View
import android.widget.SimpleCursorAdapter
import android.widget.TextView

class Selektor: SimpleCursorAdapter.ViewBinder {

    override fun setViewValue(view: View?, cursor: Cursor?, columnIndex: Int): Boolean {

        val sifre_stanica = view?.findViewById<TextView>(R.id.sifra_stanice)
        val nazivi_stanica = view?.findViewById<TextView>(R.id.naziv_stanice)
        val odredista = view?.findViewById<TextView>(R.id.odredista_sa_stanice)

        if (sifre_stanica != null) {
            if (cursor != null) {
                sifre_stanica.text=cursor.getString(cursor.getColumnIndexOrThrow("_id"))
                return true
            }
        }
        if (nazivi_stanica != null) {
            if (cursor != null) {
                nazivi_stanica.text=cursor.getString(cursor.getColumnIndexOrThrow(SQLcitac.CIR_KOLONA))
                return true
            }
        }
        if (odredista != null) {
            if (cursor != null) {
                odredista.text=cursor.getString(cursor.getColumnIndexOrThrow("staju"))
                odredista.setOnClickListener {
                    it.isSelected = !it.isSelected
                }
                return true
            }
        }
        return false
    }
}

