package com.buspljus

import android.content.Context
import android.database.Cursor
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CursorAdapter
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat

class KursorAdapterAutobus(context: Context, kursor: Cursor?): CursorAdapter(context, kursor,0) {
        override fun newView(context: Context?, cursor: Cursor?, parent: ViewGroup?): View {
            return LayoutInflater.from(context).inflate(R.layout.spisak_stanica, parent,false);
        }

        override fun bindView(view: View?, context: Context?, cursor: Cursor?) {
            view ?: return
            cursor ?: return

            val sifre_stanica = view.findViewById<TextView>(R.id.sifra_stanice)
            val nazivi_stanica = view.findViewById<TextView>(R.id.naziv_stanice)
            val odredista = view.findViewById<TextView>(R.id.odredista_sa_stanice)
            val dugmeSacuvaj = view.findViewById<ImageButton>(R.id.sacuvaj)

            val sacuvana = cursor.getInt(cursor.getColumnIndexOrThrow("sacuvana"))

            sifre_stanica.text = cursor.getString(cursor.getColumnIndexOrThrow("_id"))
            nazivi_stanica.text = cursor.getString(cursor.getColumnIndexOrThrow(SQLcitac.CIR_KOLONA))
            odredista.text = cursor.getString(cursor.getColumnIndexOrThrow("staju"))

            fun ikonica(sacuvana: Int) {
                when (sacuvana) {
                    0 -> dugmeSacuvaj.setImageDrawable(ContextCompat.getDrawable(view.context, android.R.drawable.ic_menu_save))
                    1 -> dugmeSacuvaj.setImageDrawable(ContextCompat.getDrawable(view.context, android.R.drawable.ic_delete))
                }
            }

            ikonica(sacuvana)

            dugmeSacuvaj.setOnClickListener {
                SQLcitac(view.context).sacuvajStanicu(sifre_stanica.text as String, if (sacuvana == 1) 0 else 1)
                ikonica(if (sacuvana == 1) 0 else 1)
                Glavna.adapter.changeCursor(context?.let { it1 -> SQLcitac(it1).ponoviupit() })
            }
        }
}