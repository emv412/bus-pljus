package com.buspljus.Adapteri

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.buspljus.R
import java.time.LocalTime

data class sifraNaziv(val sifra : String, val naziv : String, val vreme: LocalTime, val oznaci: Boolean)

class PrikazStanicaTrasa(context: Context, private val items: List<sifraNaziv>) : ArrayAdapter<sifraNaziv>(context, R.layout.polazak_autost, items) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View
        val viewHolder: ViewHolder

        if (convertView == null) {
            view = LayoutInflater.from(context).inflate(R.layout.polazak_autost, parent, false)
            viewHolder = ViewHolder(view)
            view.tag = viewHolder
        } else {
            view = convertView
            viewHolder = view.tag as ViewHolder
        }

        val stavka = items[position]

        with (viewHolder) {
            sifraTextView.text = stavka.sifra
            nazivTextView.text = stavka.naziv
            vremeDolTextView.text = stavka.vreme.toString()
            sifraTextView.setTextColor(context.resources.getColor(androidx.appcompat.R.color.secondary_text_default_material_dark))
            nazivTextView.setTextColor(context.resources.getColor(androidx.appcompat.R.color.secondary_text_default_material_dark))
            vremeDolTextView.setTextColor(context.resources.getColor(androidx.appcompat.R.color.secondary_text_default_material_dark))
        }

        if (stavka.oznaci) {
            with (viewHolder) {
                sifraTextView.setTextColor(context.resources.getColor(R.color.crvena))
                nazivTextView.setTextColor(context.resources.getColor(R.color.crvena))
                vremeDolTextView.setTextColor(context.resources.getColor(R.color.crvena))
            }
        }
        return view
    }

    private class ViewHolder(view: View) {
        val sifraTextView: TextView = view.findViewById(R.id.sifraSt)
        val nazivTextView: TextView = view.findViewById(R.id.imeSt)
        val vremeDolTextView: TextView = view.findViewById(R.id.vremeDol)
    }
}