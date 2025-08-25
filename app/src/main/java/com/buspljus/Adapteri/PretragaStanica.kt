package com.buspljus.Adapteri

import android.content.Context
import android.database.Cursor
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CursorAdapter
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.buspljus.Baza.PosrednikBaze
import com.buspljus.Glavna
import com.buspljus.R

class PretragaStanica(context: Context, kursor: Cursor?) : CursorAdapter(context, kursor, 0) {

    override fun newView(context: Context?, cursor: Cursor?, parent: ViewGroup?): View {
        return LayoutInflater.from(context).inflate(R.layout.pretraga_stanica_stavka, parent, false)
    }

    override fun bindView(view: View?, context: Context?, cursor: Cursor?) {
        val sifre_stanica = view?.findViewById<TextView>(R.id.sifra_stanice)
        val nazivi_stanica = view?.findViewById<TextView>(R.id.naziv_stanice)
        val odredista = view?.findViewById<TextView>(R.id.odredista_sa_stanice)
        val dugmeSacuvaj = view?.findViewById<ImageButton>(R.id.sacuvaj)

        cursor?.apply {
                val sacuvana = getInt(getColumnIndexOrThrow(PosrednikBaze.SACUVANA))
                sifre_stanica?.text = getString(getColumnIndexOrThrow(PosrednikBaze.ID_KOLONA))
                nazivi_stanica?.text = getString(getColumnIndexOrThrow(PosrednikBaze.CIR_KOLONA))
                odredista?.text = getString(getColumnIndexOrThrow("staju")).replace(", ","\n")

                fun ikonica(sacuvana: Int) {
                    when (sacuvana) {
                        0 -> dugmeSacuvaj?.setImageDrawable(ContextCompat.getDrawable(view.context, android.R.drawable.ic_menu_save))
                        1 -> dugmeSacuvaj?.setImageDrawable(ContextCompat.getDrawable(view.context, android.R.drawable.ic_delete))
                    }
                }

                ikonica(sacuvana)

            dugmeSacuvaj?.setOnClickListener {
                val novaVrednost = if (sacuvana == 1) 0 else 1
                val posrednik = PosrednikBaze(view.context.applicationContext)

                ikonica(novaVrednost)
                posrednik.sacuvajStanicu(listOf(sifre_stanica?.text.toString()), novaVrednost)
                Glavna.adapter.changeCursor(posrednik.ponoviupit())
            }
        }
    }
}
