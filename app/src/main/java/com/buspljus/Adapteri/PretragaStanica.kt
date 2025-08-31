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
import com.buspljus.Baza.Stanice
import com.buspljus.Glavna
import com.buspljus.LinijePoStanici
import com.buspljus.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
            val stationId = getString(getColumnIndexOrThrow(PosrednikBaze.ID_KOLONA))
            val nazivStaniceCIR = getString(getColumnIndexOrThrow(PosrednikBaze.CIR_KOLONA))
            
            sifre_stanica?.text = stationId
            nazivi_stanica?.text = nazivStaniceCIR

            //val priprema = Stanice(view!!.context.applicationContext).prikazLinijaNaStanici(getString(getColumnIndexOrThrow(PosrednikBaze.ID_KOLONA)))

            val linije = LinijePoStanici.linijePoStanici[stationId] ?: emptyList()

            val formatted = linije
                .groupBy { it.destination }
                .map { (dest, lines) -> "${lines.joinToString(", ") { it.lineId }} → $dest" }
                .joinToString("\n")

            odredista?.text = formatted
/*
            odredista?.text = buildString {
                priprema.entries
                    .groupBy({ it.value }, { it.key }) // value → list of keys
                    .map { (value, keys) -> keys.joinToString(", ") to value } // combine keys
                    .toMap()
                    .entries
                    .forEachIndexed { index, (keys, value) ->
                        append("$keys → $value")
                        if (index != priprema.values.toSet().size - 1) append("\n")
                    }
            }

 */

                fun ikonica(sacuvana: Int) {
                    when (sacuvana) {
                        0 -> dugmeSacuvaj?.setImageDrawable(ContextCompat.getDrawable(view.context, android.R.drawable.ic_menu_save))
                        1 -> dugmeSacuvaj?.setImageDrawable(ContextCompat.getDrawable(view.context, android.R.drawable.ic_delete))
                    }
                }

                ikonica(getInt(getColumnIndexOrThrow(PosrednikBaze.SACUVANA)))

            dugmeSacuvaj?.setOnClickListener {
                val prethodniUpit = PosrednikBaze.globalQueryData
                val posrednik = Stanice(view.context.applicationContext)

                // Pročitaj aktuelnu vrednost iz baze
                posrednik.dobaviSacuvaneStanice()
                val trenutnaVrednost = posrednik.proveriSacuvanuStanicu(sifre_stanica?.text.toString())
                val novaVrednost = if (trenutnaVrednost == 1) 0 else 1

                ikonica(novaVrednost)
                CoroutineScope(Dispatchers.IO).launch {
                    posrednik.sacuvajStanicu(sifre_stanica?.text.toString(), novaVrednost)
                    PosrednikBaze.globalQueryData = prethodniUpit
                    val noviCursor = posrednik.ponoviUpit()
                    withContext(Dispatchers.Main) {
                        Glavna.adapter.cursor?.close()
                        Glavna.adapter.changeCursor(noviCursor)
                    }
                }
            }
        }
    }
}
